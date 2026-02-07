package lang.semantics.resolvers

import lang.messages.Msg
import lang.messages.Terms
import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.builtin.PrimitivesScope
import lang.semantics.builtin.isBuiltInFuncReturnsPtr
import lang.semantics.scopes.ScopeResult
import lang.semantics.symbols.*
import lang.semantics.types.*
import lang.core.operators.OperatorType
import lang.core.SourceRange

@OptIn(ExperimentalUnsignedTypes::class)
class TypeResolver(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<ExprNode, Type>(analyzer = analyzer) {
    override fun resolve(target: ExprNode): Type {
        return when (target) {
            is LiteralNode<*> -> resolve(target)
            is NullLiteralNode -> PrimitivesScope.voidPtr
            is BaseDatatypeNode -> resolve(target)
            is IdentifierNode -> resolve(target)
            is BinOpNode -> resolve(target)
            is UnaryOpNode -> resolve(target)
            is FuncCallNode -> resolve(target)
            is DotAccessNode -> resolve(target)
            else -> ErrorType
        }.also { target attach it }
    }

    private fun resolve(target: DotAccessNode): Type {
        val baseType = resolve(target.base)
        val targetScope = baseType.declaration?.staticScope?.instanceScope

        if (!baseType.isExprType) {
            if (baseType != ErrorType)
                target.base.error(Msg.EXPECTED_VALUE_OR_REF)
            return ErrorType
        }

        if (targetScope == null) {
            target.base.error(Msg.CANNOT_FIND_DECLARATION_OF_SYM.format(Terms.SYMBOL))
            return ErrorType
        }

        return analyzer.withScope(targetScope) {
            val member = target.member

            val result = targetScope.resolve(name = member.value, asMember = true)

            result.handle(member.range) {
                target bind sym
                target.member bind sym
                resolve(member, asMember = true)
            }

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
            target.error(Msg.EXPECTED_NAMESPACE_NAME)
            return ErrorType
        }

        return analyzer.withScope(targetScope) {
            val member = target.member.identifier

            val result = targetScope.resolve(name = member.value, asMember = true)

            result.handle(member.range) {
                target bind sym
                target.member bind sym
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

    private fun pickSingleFuncSym(
        name: String,
        funcReceiverExpr: ExprNode,
        overloads: List<FuncSymbol>?
    ): FuncSymbol? {
        if (overloads.isNullOrEmpty())
            funcReceiverExpr.error(
                Msg.SymbolNotDefinedIn.format(
                    Terms.FUNCTION, name, scope.absoluteScopePath
                )
            )
        else if (overloads.size > 1)
            funcReceiverExpr.error(Msg.AMBIGUOUS_OVERLOADED_FUNCTION)
        else
            return overloads[0]

        return null
    }

    private fun resolve(target: FuncCallNode): Type {
        val receiver = target.receiver

        val argNodes = target.args
        val argTypes = resolve(argNodes)

        val receiverType = resolve(receiver)

        var params: FuncParamListSymbol? = null
        var paramTypes: List<Type> = listOf()
        var returnType: Type = ErrorType

        val sym = when (receiverType) {
            is FuncType -> {
                val decl = receiverType.funcDeclaration
                paramTypes = receiverType.paramTypes
                params = decl?.params
                returnType = receiverType.returnType
                decl
            }

            is OverloadedFuncType -> {
                val costOverloads = scope.resolveBestOverloads(receiverType.overloads, argTypes)
                val sym = pickSingleFuncSym(
                    name = receiverType.name,
                    funcReceiverExpr = receiver,
                    overloads = costOverloads
                )

                if (sym != null) {
                    paramTypes = sym.paramTypes
                    params = sym.params
                    returnType = sym.returnType
                }

                sym
            }

            is PointerType -> {
                if (receiverType.level == 1 && receiverType.base is FuncType) {
                    val decl = receiverType.base.funcDeclaration
                    paramTypes = receiverType.base.paramTypes
                    params = decl?.params
                    returnType = receiverType.base.returnType
                    decl
                } else {
                    receiver.error(Msg.SYM_NOT_A_FUNC)
                    null
                }
            }

            is ErrorType -> null

            else -> {
                receiver.error(Msg.SYM_NOT_A_FUNC)
                null
            }
        }


        for (i in paramTypes.indices) {
            val param = params?.list?.getOrNull(i)
            val paramType = paramTypes[i]
            val paramName = param?.name

            val argType = argTypes.getOrNull(i)


            if (argType == null) {
                val paramTypeStr = paramType.toString()
                val msg = if (paramName != null)
                    Msg.NoValuePassedForParameter.format(paramName = paramName, paramTypeStr)
                else
                    Msg.NoValuePassedForParameter.format(paramIndex = i+1, paramTypeStr)

                receiver.error(msg)
                continue
            }

            if (argType != ErrorType && !argType.canCastTo(paramType)) {
                val msg = Msg.MismatchExpectedActual
                    .format(
                        Terms.ARGUMENT_TYPE,
                        paramType.toString(),
                        argType.toString()
                    )
                argNodes.getOrNull(i)?.error(msg)
                continue
            }
        }

        target bind sym
        return returnType.also { target attach it }
    }

    fun resolve(target: BaseDatatypeNode, isNamespaceCtx: Boolean = false): Type {
        return when (target) {
            is DatatypeNode -> resolve(target, isNamespaceCtx)
            is ScopedDatatypeNode -> resolve(target)

            is AutoDatatypeNode,
            is VoidDatatypeNode -> PrimitivesScope.void

            is FuncDatatypeNode -> resolve(target)
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

    private fun Symbol.toTypeForNode(
        target: ExprNode,
        isNamespaceCtx: Boolean = false
    ): Type {
        return when (this) {
            is VarSymbol -> type.setFlags(isMutable = isMutable, isExprType = true, isLvalue = true)
            is ConstValueSymbol -> type.setFlags(isExprType = true, isLvalue = true)
            is PrimitiveTypeSymbol -> type.setFlags(isExprType = false)
            is NamespaceSymbol -> if (isNamespaceCtx) {
                NamespaceType(name = name, declaration = this)
            } else {
                target.error(Msg.F_SYM_NOT_ALLOWED_HERE.format(name))
            }

            is TypeSymbol -> createUserType(sym = this).setFlags(isExprType = isNamespaceCtx)
            is FuncSymbol -> toFuncType()
            is OverloadedFuncSymbol -> OverloadedFuncType(
                name = name,
                overloads = overloads,
                flags = TypeFlags(isExprType = true)
            )

            else -> symNotDefinedInError(this.name, scope.absoluteScopePath, target.range)
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


    /*
    private fun resolve(target: DatatypeNode, isNamespace: Boolean = false): Type {
        val name = target.identifier
        val result = scope.resolve(name.value)

        return result.handle(name.range) {
            target bind sym

            val pointerLevel = target.ptrLvl

            val type = when (sym) {
                is PrimitiveTypeSymbol -> sym.type

                is NamespaceSymbol -> {
                    if (isNamespace)
                        NamespaceType(
                            name = sym.name,
                            declaration = sym
                        )
                    else
                        target.error(Msg.EXPECTED_A_VALUE.format(sym.name))
                }

                is TypeSymbol -> {
                    createUserType(
                        sym = sym,
                        templateArgs = resolveTemplateArgs(target.typeNames)
                    )
                }

                is TypedefSymbol -> {
                    sym.type
                }

                else -> {
                    name.error(::symNotDefinedError)
                    return@handle ErrorType
                }
            }

            return@handle type.applyTypeModifiers(
                pointerLevel = pointerLevel,
                isConst = target.isConst || type.isConst,
                isReference = target.isReference
            )
        }
    }

        private fun resolveIdentifierWithSym(target: IdentifierNode, sym: Symbol, isNamespace: Boolean = false): Type {

            target bind sym

            return when (sym) {
                is VarSymbol -> sym.type.setFlags(
                    isMutable = sym.isMutable,
                    isExprType = true,
                    isLvalue = true
                )

                is ConstValueSymbol -> {
                    sym.type.setFlags(
                        isExprType = true,
                        isLvalue = true
                    )
                }

                is PrimitiveTypeSymbol -> sym.type.setFlags(
                    isExprType = false
                )

                is NamespaceSymbol -> {
                    if (isNamespace)
                        NamespaceType(
                            name = sym.name,
                            declaration = sym
                        )
                    else
                        target.error(Msg.F_SYM_NOT_ALLOWED_HERE.format(sym.name))
                }

                is TypeSymbol -> createUserType(sym = sym)
                    .setFlags(isExprType = isNamespace)

                is FuncSymbol -> sym.toFuncType()

                is OverloadedFuncSymbol -> {
                    OverloadedFuncType(
                        name = sym.name,
                        overloads = sym.overloads,
                        flags = TypeFlags(
                            isExprType = true
                        )
                    )
                }


                else -> target.error(::symNotDefinedError)
            }.also { target attach it }
        }
    */

    private fun resolve(target: IdentifierNode, asMember: Boolean = false): Type {
        val result = scope.resolve(name = target.value, asMember = asMember)

        return result.handle(target.range) {
            resolveIdentifierWithSym(target, sym)
        }
    }

    private fun createUserType(
        sym: TypeSymbol,
        templateArgs: List<TemplateArg> = emptyList()
    ) =
        UserType(
            name = sym.name,
            templateArgs = templateArgs,
            declaration = sym,
            flags = TypeFlags(isExprType = false)
        )

    fun ExprNode.constFoldAndBind(): ConstValueSymbol? {
        val constValue = analyzer.constResolver.resolve(this)

        return if (constValue != null) {
            ConstValueSymbol
                .from(constValue)
                .also { this bind it }
        } else null
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

        if (rightType != ErrorType && !rightType.isExprType) {
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

        return resolveOperFunc(target, leftType, rightType, operator)
    }

    private fun resolveOperFunc(
        target: ExprNode,
        leftType: Type,
        rightType: Type,
        operator: OperatorType,
    ): Type {
        val operScope = when (leftType) {
            is PrimitiveType,
            is UserType ->
                leftType.declaration?.staticScope?.instanceScope

            else -> scope
        }

        if (operScope == null) {
            return target.error(Msg.CANNOT_FIND_DECLARATION_OF_SYM.format(leftType.toString()))
        }

        val argTypes = listOf(leftType, rightType)
        val result = operScope.resolveOperFunc(operator = operator, argTypes = argTypes)

        return result.handle(target.range) {
            val operFunc = sym as? FuncSymbol
                ?: return@handle target.error(Msg.CANNOT_FIND_DECLARATION_OF_SYM.format(operator.name))

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

            leftType.canCastTo(rightType) -> rightType

            else -> {
                if (!leftType.canCastTo(rightType)) {
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

    private fun resolveAssign(
        target: BinOpNode,
        leftType: Type,
        rightType: Type
    ): Type {
        when {
            !leftType.isLvalue ->
                target.left.error(Msg.EXPECTED_VARIABLE_ACTUAL_VALUE)

            leftType.isConst ->
                target.error(Msg.ASSIGNMENT_TO_CONSTANT_VARIABLE)

            !leftType.isMutable ->
                target.error(Msg.ASSIGNMENT_TO_IMMUTABLE_VARIABLE)
        }

        if (!rightType.canCastTo(leftType)) {
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
            isLvalue = true,
            isConst = leftType.isConst,
            isMutable = leftType.isMutable
        ).also { target attach it }
    }


    private fun Type.applyTypeModifiers(
        pointerLevel: Int,
        isConst: Boolean,
        isReference: Boolean
    ): Type {
        val type = if (pointerLevel > 0) PointerType(base = this) else this

        return type.setFlags(
            isConst = isConst,
            isLvalue = isReference,
            isExprType = false
        )
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


    private fun resolve(target: UnaryOpNode): Type {
        return ErrorType
    }

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

    fun resolveBestOverloadForType(target: ExprNode, type: FuncType, overloads: List<FuncSymbol>): FuncSymbol? {
        val bestFunc = scope.resolveExactOverload(
            overloads = overloads,
            types = type.paramTypes,
            returnType = type.returnType
        )

        if (bestFunc == null) {
            val msg = Msg.F_NONE_OF_N_CANDIDATES_APPLICABLE_FOR_TYPE
                .format(overloads.size, type.toString())

            target.error(msg)
            return null
        }

        return bestFunc
    }

    fun resolveForType(target: ExprNode, type: Type): Type {
        val initializerType = analyzer.typeResolver.resolve(target)

        if (initializerType is OverloadedFuncType) {
            if (type !is FuncType) {
                return target.error(Msg.AMBIGUOUS_OVERLOADED_FUNCTION)
            }

            val bestFuncSym = resolveBestOverloadForType(target, type, initializerType.overloads)

            target bind bestFuncSym

            return bestFuncSym?.toFuncType() ?: ErrorType
        }

        if (initializerType != ErrorType && !initializerType.canCastTo(type))
            target.error(
                Msg.MismatchExpectedActual.format(
                    Terms.TYPE,
                    type.toString(),
                    initializerType.toString()
                )
            )

        return type
    }

    fun ScopeResult.handle(range: SourceRange?, onSuccess: ScopeResult.Success<*>.() -> Type): Type {
        return when (this) {
            is ScopeResult.Error -> {
                if (range != null)
                    analyzer.scopeError(error, range)

                ErrorType
            }

            is ScopeResult.Success<*> -> {
                onSuccess()
            }
        }
    }

/*
    fun ScopeResult.handle(onSuccess: ScopeResult.Success<*>.() -> Type) =
        this.handle(null, onSuccess)
*/
}