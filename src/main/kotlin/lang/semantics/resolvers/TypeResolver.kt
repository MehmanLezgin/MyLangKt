package lang.semantics.resolvers

import lang.core.SourceRange
import lang.core.Utils.toInt
import lang.core.operators.OperatorType
import lang.messages.Msg
import lang.messages.Terms
import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.builtin.PrimitivesScope
import lang.semantics.builtin.isBuiltInFuncReturnsPtr
import lang.semantics.scopes.*
import lang.semantics.symbols.*
import lang.semantics.types.*

@OptIn(ExperimentalUnsignedTypes::class)
class TypeResolver(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<ExprNode, Type>(analyzer = analyzer) {
    override fun resolve(target: ExprNode): Type {
        return when (target) {
            is LiteralNode<*> -> resolve(target)
            is NullLiteralNode -> resolveNullLiteral()

            is ThisIdentifierNode -> resolve(target)
            is SuperIdentifierNode -> resolve(target)
            is IdentifierNode -> resolve(target)

            is VoidNode -> PrimitivesScope.void

            is BaseDatatypeNode -> resolve(target)
            is BinOpNode -> resolve(target)
            is UnaryOpNode -> resolve(target)
            is FuncCallNode -> resolve(target)
            is DotAccessNode -> resolve(target)
            is ReturnStmtNode -> resolve(target)
            is SizeofNode,
            is AlignofNode,
            is OffsetofNode -> resolveConst(target)

            is BlockNode -> resolve(target)

            else -> ErrorType
        }.also { target attach it }
    }

    private fun resolve(target: BlockNode): Type {
        if (target.nodes.isEmpty())
            return UnresolvedType

        val last = target.nodes.last()
        return last.getResolvedType() ?: UnresolvedType
    }

    private fun resolveConst(target: ExprNode): Type {
        val a = target.constFoldAndBind()
        return a?.type ?: ErrorType
    }

    private fun resolveCurrentScopeType(target: ExprNode): UserType? {
        val typeScope = scope.getEnclosingScope<InstanceScope>()
            ?.parent

        if (typeScope !is ClassScope) {
            target.error(
                Msg.SymbolNotDefinedIn.format(
                    name = Terms.THIS,
                    scopeName = null
                )
            )

            return null
        }

        return typeScope.ownerSymbol.type
    }

    private fun resolve(target: ThisIdentifierNode): Type {
        val type = resolveCurrentScopeType(target)
            ?: return ErrorType

        return PointerType(
            base = type,
            level = 1,
            flags = TypeFlags(
                isConst = true,
                isLvalue = false,
                isExprType = true
            )
        )
    }


    private fun resolve(target: SuperIdentifierNode): Type {
        val type = resolveCurrentScopeType(target)
            ?: return ErrorType

        val superType = type.declaration?.superType

        if (superType !is UserType)
            return target.error(
                Msg.TypeDoesNotHaveSuper.format(
                    typeName = type.name
                )
            )

        return PointerType(
            base = superType,
            level = 1,
            flags = TypeFlags(
                isConst = true,
                isLvalue = false,
                isExprType = true
            )
        )
    }

    private fun resolveNullLiteral() =
        PrimitivesScope.voidPtr.setFlags(
            isExprType = true
        )

    private fun resolve(target: ReturnStmtNode): Type {
        val exprType = resolve(target.expr)
        val funcScope = scope.getEnclosingScope<FuncScope>()
            ?: return target.error(Msg.EXPECTED_FUNC_DECL)

        val funcRetType = funcScope.funcSymbol.returnType

        if (funcRetType != UnresolvedType && convert(exprType, funcRetType).notExists()) {
            return target.expr.error(
                Msg.MismatchExpectedActual.format(
                    mismatchKind = Terms.RETURN_TYPE,
                    expected = funcRetType.toString(),
                    actual = exprType.toString()
                )
            )
        }

        return funcRetType
    }

    private fun resolve(target: DotAccessNode): Type {
        var baseType = resolve(target.base)

        if (!baseType.isExprType) {
            if (baseType != ErrorType)
                target.base.error(Msg.EXPECTED_VALUE_OR_REF)
            return ErrorType
        }

        if (baseType is PointerType) {
            if (baseType.level != 1) {
                target.base.error(Msg.CANNOT_DOT_ACCESS_MULTI_LEVEL_POINTER)
                return ErrorType
            }

            baseType = baseType.base.setFlags(isExprType = true)
        }

        val targetScope = baseType.declaration?.staticScope?.instanceScope



        if (targetScope == null) {
            target.base.error(Msg.CANNOT_FIND_DECLARATION_OF_SYM.format(baseType.toString()))
            return ErrorType
        }

        val member = target.member

        val fromScope = when (target.base) {
            is SuperIdentifierNode -> scope.getEnclosingScope<InstanceScope>()
            else -> scope
        } ?: scope

        return analyzer.withScope(targetScope) {
            resolve(member, fromScope = fromScope, asMember = true)
        }.also {
            target attach it
            target.member attach it
        }
    }

    private fun resolveNamespace(target: ExprNode): Type {
        return when (target) {
            is ScopedDatatypeNode -> resolve(target, isNamespaceCtx = true)
            is QualifiedDatatypeNode -> resolve(target, isNamespaceCtx = true)
            else -> resolve(target)
        }
    }

    private fun resolve(target: ScopedDatatypeNode, isNamespaceCtx: Boolean = false): Type {
        val type = resolveNamespace(target.base)

        val targetScope = type.declaration?.staticScope

        if (type.isExprType || targetScope == null) {
            if (type != ErrorType)
                target.error(Msg.EXPECTED_MODULE_NAME)
            return ErrorType
        }

        val member = target.member.identifier

        val result = targetScope.resolve(
            name = member.value,
            from = scope,
            asMember = true
        )

        return result.handle(member.range) {
            target bind sym
            target.member bind sym

            analyzer.withScope(targetScope) {
                resolveIdentifierWithSym(member, sym, isNamespaceCtx)
            }
        }.also {
            target attach it
            target.member attach it
        }
    }

    private fun resolve(target: List<ExprNode>): List<Type> {
        return target.map { resolve(it) }
    }

    private fun resolve(target: FuncCallNode): Type {
        val receiver = target.receiver

        val argNodes = target.args
        val argTypes = resolve(argNodes)

        val receiverType = resolve(receiver)

        var params: FuncParamListSymbol? = null
        var returnType: Type = ErrorType

        if (argTypes.any { it == ErrorType }) {
            return returnType.setFlags(
                isExprType = true,
            ).also { target attach it }
        }

        val sym = when (receiverType) {
            is ErrorType -> null

            is PointerType -> {
                if (receiverType.level == 1 && receiverType.base is FuncType) {
                    val decl = receiverType.base.funcDeclaration
                    params = decl?.params
                    returnType = receiverType.base.returnType
                    decl
                } else {
                    receiver.error(Msg.SYM_NOT_A_FUNC)
                    null
                }
            }

            is OverloadedFuncType -> {
                val typeScope = receiverType.overloadedFuncSym.accessScope
                val fromScope = scope

                val funcSym = analyzer.withScope(typeScope) {
                    analyzer.overloadResolver.resolveFunc(
                        overloadedFunc = receiverType.overloadedFuncSym,
                        from = fromScope,
                        argTypes = argTypes
                    ).handle<FuncSymbol>(target.range) {
                        sym as? FuncSymbol
                    }
                }

                if (funcSym != null) {
                    funcSym.paramTypes
                    params = funcSym.params
                    returnType = funcSym.returnType
                }

                funcSym
            }

            else -> {
                val decl = receiverType.declaration
                    ?: return receiver.error(Msg.SYM_NOT_A_FUNC)

                val typeScope = decl.staticScope.instanceScope
                val fromScope = scope

                val funcSym = analyzer.withScope(typeScope) {
                    analyzer.overloadResolver.resolveConstructor(
                        argTypes = argTypes,
                        from = fromScope,
                    ).handle<FuncSymbol>(target.range) {
                        sym as? FuncSymbol
                    }
                }

                if (funcSym != null) {
                    params = funcSym.params
                    returnType = receiverType
                }

                funcSym
            }
        }

        validateFuncArgs(
            receiver = receiver,
            argNodes = argNodes,
            argTypes = argTypes,
            params = params,
        )


        target bind sym

        return returnType.setFlags(
            isExprType = true,
        ).also { target attach it }
    }

    private fun validateFuncArgs(
        receiver: ExprNode,
        argNodes: List<ExprNode>,
        argTypes: List<Type>,
        applyOffset: Boolean = false,
        params: FuncParamListSymbol?,
    ) {
        val paramTypes = params?.list ?: return
        val argOffset = applyOffset.toInt()

        for (i in paramTypes.indices) {
            val param = params.list.getOrNull(i)
            val paramType = paramTypes[i].type
            val paramName = param?.name

            val argType = argTypes.getOrNull(i + argOffset)


            if (argType == null) {
                val paramTypeStr = paramType.toString()
                val msg = if (paramName != null)
                    Msg.NoValuePassedForParameter.format(paramName = paramName, paramTypeStr)
                else
                    Msg.NoValuePassedForParameter.format(paramIndex = i + 1, paramTypeStr)

                receiver.error(msg)
                continue
            }

            if (argType != ErrorType && convert(argType, paramType).notExists()) {
                val msg = Msg.MismatchExpectedActual
                    .format(
                        mismatchKind = Terms.ARGUMENT_TYPE,
                        expected = paramType.toString(),
                        actual = argType.toString()
                    )
                argNodes.getOrNull(i)?.error(msg)
                continue
            }
        }
    }

    fun resolve(target: BaseDatatypeNode, isNamespaceCtx: Boolean = false): Type {
        return when (target) {
            is DatatypeNode -> resolve(target, isNamespaceCtx)
            is ScopedDatatypeNode -> resolve(target)

            is AutoDatatypeNode,
            is VoidDatatypeNode -> PrimitivesScope.void

            is FuncDatatypeNode -> resolve(target)
            is MethodDatatypeNode -> resolve(target)
            is ErrorDatatypeNode -> ErrorType
            else -> ErrorType
        }.also { target attach it }
    }

    private fun resolve(target: FuncDatatypeNode): Type {
        return FuncType(
            paramTypes = target.paramDatatypes
                .map { resolve(it) },
            returnType = resolve(target.returnDatatype)
        ).applyTypeModifiers(
            pointerLevel = target.ptrLvl,
            isConst = target.isConst,
            isReference = target.isReference
        ).also { target attach it }
    }

    private fun resolve(target: MethodDatatypeNode): Type {
        val ownerType = resolve(target.ownerDatatype)
        val funcType = resolve(target.funcDatatype)

        if (ownerType == ErrorType || funcType == ErrorType)
            return ErrorType
//            return target.funcDatatype.error(Msg.EXPECTED_FUNC_TYPE)

        if (funcType is FuncType)
            return funcType.toMethodType(ownerType = ownerType)

        if (funcType is PointerType) {
            val base = funcType.base as? FuncType
                ?: return target.funcDatatype.error(Msg.EXPECTED_FUNC_TYPE)


            return PointerType(
                base = base.toMethodType(ownerType = ownerType),
                level = funcType.level,
                flags = funcType.flags
            )
        }

        return target.funcDatatype.error(Msg.EXPECTED_FUNC_TYPE)
//            return FuncType(
//                paramTypes = target.paramDatatypes
//                    .map { resolve(it) },
//                returnType = resolve(target.returnDatatype)
//            ).applyTypeModifiers(
//                pointerLevel = target.ptrLvl,
//                isConst = target.isConst,
//                isReference = target.isReference
//            ).also { target attach it }
    }

    private fun Symbol.toTypeForNode(
        target: ExprNode,
        isNamespaceCtx: Boolean = false
    ): Type {
        return when (this) {
            is VarSymbol -> {
                if (type is UnresolvedType)
                    target.error(Msg.AutoVarCannotBeInferred.format(this.name))
                else
                    type.setFlags(isMutable = isMutable, isExprType = true, isLvalue = true)
            }

            is ConstValueSymbol -> type.setFlags(isExprType = true, isLvalue = true)
            is PrimitiveTypeSymbol -> primitiveType.setFlags(isExprType = false)
            is ModuleSymbol -> if (isNamespaceCtx) {
                NamespaceType(name = name, declaration = this)
            } else {
                target.error(Msg.F_SYM_NOT_ALLOWED_HERE.format(name))
            }

            is TypeSymbol -> this.type.setFlags(isExprType = isNamespaceCtx)
            is FuncSymbol -> toFuncType()

            is OverloadedMethodSymbol -> OverloadedMethodType(
                ownerType = this.accessScope.parent.ownerSymbol.type,
                name = name,
                overloadedFuncSym = this,
                flags = TypeFlags(isExprType = true)
            )

            is OverloadedFuncSymbol -> OverloadedFuncType(
                name = name,
                overloadedFuncSym = this,
                flags = TypeFlags(isExprType = true)
            )


            else ->
                target.error(
                    Msg.SymbolNotDefinedIn.format(
                        name = this.name,
                        scopeName = scope.absoluteScopePath
                    )
                )

        }.also { target attach it }
    }

    private fun resolveIdentifierWithSym(
        target: IdentifierNode,
        sym: Symbol,
        isNamespace: Boolean = false
    ): Type {
        target bind sym
        return sym.toTypeForNode(target, isNamespace)
    }

    private fun resolve(target: DatatypeNode, isNamespace: Boolean = false): Type {
        val result = scope.resolve(target.identifier.value)

        return result.handle(target.identifier.range) {
            target bind sym

            val baseType = sym.toTypeForNode(target.identifier, isNamespace)

            baseType.applyTypeModifiers(
                pointerLevel = target.ptrLvl,
                isConst = target.isConst || baseType.isConst,
                isReference = target.isReference
            )
        }
    }

    fun resolve(
        target: IdentifierNode,
        fromScope: Scope = scope,
        asMember: Boolean = false,
        isNamespace: Boolean = false
    ): Type {
        when (target) {
            is ThisIdentifierNode -> return resolve(target)
            is SuperIdentifierNode -> return resolve(target)
        }

        val result = scope.resolve(name = target.value, from = fromScope, asMember = asMember)

        return result.handle(target.range) {
            resolveIdentifierWithSym(target, sym, isNamespace)
        }
    }

    fun ExprNode.constFoldAndBind(): ConstValueSymbol? {
        val constValue = analyzer.constResolver.resolve(this)

        return if (constValue != null) {
            ConstValueSymbol
                .from(constValue)
                .also { this bind it }
        } else null
    }

    private fun resolve(target: UnaryOpNode): Type {
        target.constFoldAndBind()?.let {
            return it.type
        }

        val operand = target.operand
        val operandType = resolve(operand)

        if (operandType != ErrorType && !operandType.isExprType) {
            operand.error(Msg.EXPECTED_VALUE_OR_REF)
            return ErrorType
        }

        when (target.operator) {
            UnaryOpType.INCREMENT,
            UnaryOpType.DECREMENT -> {
                handleVarChange(operand, operandType)
                    ?.let { return it }

                target bind operand.getResolvedSymbol()

                return operandType.setFlags(
                    isExprType = true,
                    isLvalue = false
                ).also { target attach it }
            }

            UnaryOpType.NEW -> {}
            UnaryOpType.DELETE -> {}
            UnaryOpType.ADDRESS_OF -> {
                if (!operandType.isLvalue)
                    return operand.error(Msg.EXPECTED_VARIABLE_ACTUAL_VALUE)

                if (!operandType.isExprType)
                    return operand.error(Msg.EXPECTED_VARIABLE_ACTUAL_TYPE_NAME)

                return operandType.createPtr()
                    .also { target attach it }
            }

            UnaryOpType.INDIRECTION -> {
                if (!operandType.isExprType || operandType !is PointerType)
                    return operand.error(Msg.EXPECTED_A_POINTER_VALUE)

                return operandType.createDeRef()
                    .also { target attach it }
            }

            UnaryOpType.BITWISE_NOT -> {}
            UnaryOpType.IS -> {}
            UnaryOpType.NON_NULL_ASSERT -> {}
            else -> {}
        }

        val operator = target.tokenOperatorType
        val argTypes = listOf(operandType)

        return resolveOperFunc(
            target = target,
            argTypes = argTypes,
            args = listOf(target.operand),
            operator = operator
        )
    }

    private fun resolve(target: BinOpNode): Type {
        target.constFoldAndBind()?.let {
            return it.type
        }

        var leftType = resolve(target.left)
        var rightType = resolve(target.right)

        if (leftType != ErrorType && !leftType.isExprType) {
            target.left.error(Msg.EXPECTED_VALUE_OR_REF)
            leftType = ErrorType
        }

        if (rightType != ErrorType && !rightType.isExprType && target.operator != BinOpType.CAST) {
            target.right.error(Msg.EXPECTED_VALUE_OR_REF)
            rightType = ErrorType
        }

        if (leftType == ErrorType || rightType == ErrorType)
            return ErrorType

        when (target.operator) {
            BinOpType.ASSIGN -> return resolveAssign(target, leftType, rightType)
            BinOpType.CAST -> return resolveCast(target, leftType, rightType)
            BinOpType.IS -> return resolveIs(target, leftType, rightType)
            else -> {}
        }

        val operator = target.tokenOperatorType

        val argTypes = listOf(leftType, rightType)

        return resolveOperFunc(
            target = target,
            argTypes = argTypes,
            args = listOf(target.right),
            operator = operator
        )
    }

    private fun resolveOperFunc(
        target: ExprNode,
        argTypes: List<Type>,
        args: List<ExprNode>,
        operator: OperatorType,
    ): Type {
        val leftType = argTypes.getOrNull(0) ?: return ErrorType
        val isStatic: Boolean

        val operScope = when (leftType) {
            is PrimitiveType,
            is UserType -> {
                isStatic = false
                leftType.declaration?.staticScope?.instanceScope
            }

            else -> {
                isStatic = true
                scope
            }
        }

        if (operScope == null)
            return target.error(Msg.CANNOT_FIND_DECLARATION_OF_SYM.format(leftType.toString()))

        val fromScope = scope

        val result = analyzer.withScope(operScope) {
            analyzer.overloadResolver.resolveOperFunc(
                operator = operator,
                from = fromScope,
                argTypes = argTypes,
                isStatic = isStatic
            )
        }

        return result.handle(target.range) {
            val operFunc = sym as? FuncSymbol
                ?: return@handle target.error(Msg.CANNOT_FIND_DECLARATION_OF_SYM.format(operator.name))

            validateFuncArgs(
                receiver = target,
                argNodes = args,
                argTypes = argTypes,
                applyOffset = !isStatic,
                params = operFunc.params
            )

            val returnType = when {
                operFunc.isBuiltInFuncReturnsPtr() -> leftType
                else -> operFunc.returnType
            }

            target bind operFunc

            val isConst = operFunc.returnType.isConst

            returnType.setFlags(
                isConst = isConst,
                isExprType = true,
                isLvalue = false,
                isMutable = false
            ).also { target attach it }
        }
    }

    private fun resolveIs(
        target: BinOpNode,
        leftType: Type,
        rightType: Type
    ): Type {
        if (!leftType.isExprType)
            target.left.error(Msg.EXPECTED_AN_EXPRESSION)

        if (rightType.isExprType)
            target.right.error(Msg.EXPECTED_TYPE_NAME)

        return PrimitivesScope.bool.setFlags(
            isConst = false,
            isExprType = true
        ).also { target attach it }
    }

    private fun resolveCast(
        target: BinOpNode,
        leftType: Type,
        rightType: Type
    ): Type {
        val type = when {
            rightType.isExprType -> {
                target.right.error(Msg.EXPECTED_TYPE_NAME)
                ErrorType
            }

            leftType is OverloadedFuncType -> {
                resolveForType(target.left, rightType)
            }

            else -> {
                if (convert(leftType, rightType).notExists()) {
                    val msg = Msg.CannotCastType.format(
                        leftType.toString(),
                        rightType.toString()
                    )

                    target.error(msg)
                }
                rightType
            }
        }

        type.also { target attach it }


        return type.setFlags(
            isExprType = true
        )
    }

    private fun handleVarChange(target: ExprNode, operandType: Type): ErrorType? {
        return when {
            !operandType.isLvalue ->
                target.error(Msg.EXPECTED_VARIABLE_ACTUAL_VALUE)

            operandType.isConst ->
                target.error(Msg.ASSIGNMENT_TO_CONSTANT_VARIABLE)

            !operandType.isMutable ->
                target.error(Msg.ASSIGNMENT_TO_IMMUTABLE_VARIABLE)

            else -> null
        }
    }

    private fun resolveAssign(
        target: BinOpNode,
        leftType: Type,
        rightType: Type
    ): Type {
        handleVarChange(target.left, leftType)
            ?.let { return it }

        if (leftType == ErrorType || rightType == ErrorType)
            return leftType

        if (convert(rightType, leftType).notExists()) {
            target.error(
                Msg.MismatchExpectedActual.format(
                    Terms.TYPE,
                    leftType.toString(),
                    rightType.toString()
                )
            )
        }

        target bind target.left.getResolvedSymbol()

        return leftType.setFlags(
            isExprType = true,
            isLvalue = true
        ).also { target attach it }
    }


    private fun Type.applyTypeModifiers(
        pointerLevel: Int,
        isConst: Boolean,
        isReference: Boolean
    ): Type {
        var type = this.setFlags(
            isConst = isConst,
            isLvalue = isReference,
            isExprType = false
        )

        if (pointerLevel > 0)
            type = PointerType(base = type, level = pointerLevel)

        return type
    }

    private fun Type.createPtr(): Type {
        return when (this) {
            is PointerType -> PointerType(
                base = base,
                level = level + 1,
                flags = flags
            )

            else -> PointerType(
                base = this
            )
        }.setFlags(
            isExprType = true,
            isLvalue = false
        )
    }

    private fun PointerType.createDeRef(): Type {
        return when (level) {
            1 -> base.setFlags(
                isConst = base.isConst,
                isExprType = true,
                isLvalue = true,
                isMutable = true
            )

            else -> PointerType(
                base = base,
                level = level - 1,
                flags = flags
            )
        }
    }

    /*
        private fun resolveTemplateArgs(typeNames: List<ExprNode>?): List<TemplateArg> {
            if (typeNames == null) return emptyList()

            return typeNames.map {
                val type = resolve(target = it)

                if (type == ErrorType || it is BaseDatatypeNode)
                    return@map TemplateArg.ArgType(type = type)

                if (type.isExprType || type.isConst) {
                    val constValue = analyzer.constResolver.resolve(target = it)
                    return@map if (constValue != null)
                        TemplateArg.ArgConstValue(value = constValue)
                    else {
                        it.error(Msg.EXPECTED_CONST_VALUE)
                        TemplateArg.ArgType(ErrorType)
                    }
                }

                return@map TemplateArg.ArgType(type = type)
            }
        }
    */


    /*private fun resolve(target: UserTypeSymbol): Type {
        when (target) {

        }
    }*/


    private fun resolve(target: LiteralNode<*>): Type {
        val type = when (target) {
            is LiteralNode.BooleanLiteral -> PrimitivesScope.boolConst
            is LiteralNode.CharLiteral -> PrimitivesScope.charConst
            is LiteralNode.DoubleLiteral -> PrimitivesScope.float64Const
            is LiteralNode.FloatLiteral -> PrimitivesScope.float32Const
            is LiteralNode.IntLiteral -> PrimitivesScope.int32Const
            is LiteralNode.LongLiteral -> PrimitivesScope.int64Const
            is LiteralNode.StringLiteral -> PrimitivesScope.constCharPtr
            is LiteralNode.UIntLiteral -> PrimitivesScope.uint32Const
            is LiteralNode.ULongLiteral -> PrimitivesScope.uint64Const
        }.setFlags(isExprType = true, isConst = true)

        target.constFoldAndBind()

        return type
    }

    private fun <T : Symbol> List<T>.filterIsAccessible(
        targetScope: Scope,
        fromScope: Scope,
        asMember: Boolean = false
    ): List<T> {
        return filter { sym ->
            targetScope.isSymAccessibleFrom(
                sym = sym,
                from = fromScope,
                asMember = asMember
            )
        }
    }

    fun resolveForType(target: ExprNode, type: Type): Type {
        val initializerType = analyzer.typeResolver.resolve(target)

        if (initializerType is OverloadedFuncType) {
            val overloaded = initializerType.overloadedFuncSym
            val overloadsList: List<FuncSymbol> = overloaded.overloads
            val accessScope = overloaded.accessScope

            when (type) {
                is UnresolvedType -> {
                    val accessible = overloadsList.filterIsAccessible(
                        targetScope = accessScope,
                        fromScope = scope,
                        asMember = true
                    )

                    return when {
                        accessible.isEmpty() -> target.error(
                            Msg.SymbolNotDefinedIn.format(
                                itemKind = Terms.FUNCTION,
                                name = overloaded.name,
                                scopeName = null
                            )
                        )

                        accessible.size > 1 -> target.error(
                            Msg.AmbiguousOverloadedFunc.format(list = overloadsList)
                        )

                        else -> {
                            val sym = accessible.first()
                            target bind sym
                            sym.toFuncType()
                        }
                    }
                }

                is FuncType -> {
                    val typeScope = overloaded.accessScope
                    val fromScope = scope

                    val bestFuncSym = analyzer.withScope(typeScope) {
                        analyzer.overloadResolver.resolveFunc(
                            overloadedFunc = overloaded,
                            from = fromScope,
                            argTypes = type.paramTypes
                        ).handle<FuncSymbol>(target.range) {
                            sym as? FuncSymbol
                        }
                    }

                    val funcType = bestFuncSym?.toFuncType()
                        ?: return ErrorType


                    if (convert(funcType, type).notExists())
                        return target.error(
                            Msg.CannotCastType.format(
                                from = funcType.toString(),
                                to = type.toString()
                            )
                        )


                    target bind bestFuncSym

                    return funcType
                }
            }
        }

        if (type is UnresolvedType)
            return initializerType

        if (initializerType == ErrorType || type == ErrorType)
            return type

        if (convert(initializerType, type).notExists())
            target.error(
                Msg.MismatchExpectedActual.format(
                    Terms.TYPE,
                    type.toString(),
                    initializerType.toString()
                )
            )

        return type
    }

    private fun convert(fromType: Type, toType: Type) =
        analyzer.convertResolver.convert(fromType, toType)

    fun ScopeResult.handle(range: SourceRange?, onSuccess: ScopeResult.Success<*>.() -> Type): Type {
        return handle<Type?>(range, onSuccess) ?: ErrorType
    }
}