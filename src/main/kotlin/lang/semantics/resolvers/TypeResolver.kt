package lang.semantics.resolvers

import lang.messages.Messages
import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.symbols.*
import lang.semantics.types.*
import lang.tokens.OperatorType

@OptIn(ExperimentalUnsignedTypes::class)
class TypeResolver(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<ExprNode, Type>(analyzer = analyzer) {
    override fun resolve(target: ExprNode): Type {
        return when (target) {
            is LiteralNode<*> -> resolve(target)
            is NullLiteralNode -> PrimitiveTypes.voidPtr
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

        if (!baseType.isExprType || targetScope == null) {
            target.base.error(Messages.EXPECTED_A_VALUE)
            return ErrorType
        }

        return analyzer.withScope(targetScope) {
            val member = target.member

            val sym = targetScope.resolve(name = member.value, asMember = true)

            target bind sym
            target.member bind sym

            resolve(member, asMember = true)

        }.also {
            target attach it
            target.member attach it
        }
    }

    private fun resolve(target: ScopedDatatypeNode): Type {
        val type = resolve(target.base)

        val targetScope = type.declaration?.staticScope

        if (type.isExprType || targetScope == null) {
            target.error(Messages.EXPECTED_NAMESPACE_NAME)
            return ErrorType
        }

        return analyzer.withScope(targetScope) {
            val member = target.member.identifier

//            val memberType = resolve(member, asMember = true)

            val sym = targetScope.resolve(name = member.value, asMember = true)
                ?: return@withScope semanticError(Messages.F_SYMBOL_NOT_DEFINED_CUR, target.pos)

            target bind sym
            target.member bind sym

            resolveIdentifierWithSym(member, sym)
//            memberType

        }.also {
            target attach it
            target.member attach it
        }
    }

    private fun resolve(target: List<ExprNode>): List<Type> {
        return target.map { resolve(it) }
    }

    private fun resolveSingleFuncSym(
        funcNameExpr: ExprNode,
        overloads: List<FuncSymbol>?
    ): FuncSymbol? {
        if (overloads.isNullOrEmpty())
            funcNameExpr.error(Messages.F_FUNC_NOT_DEFINED)
        else if (overloads.size > 1)
            funcNameExpr.error(Messages.AMBIGUOUS_OVERLOADED_FUNCTION)
        else
            return overloads[0]

        return null
    }

    private fun resolve(target: FuncCallNode): Type {
        val name = target.name
        if (name !is IdentifierNode)
            return name.error(Messages.EXPECTED_FUNC_NAME)

        val argNodes = target.args
        val argTypes = resolve(argNodes)

        val costOverloads = scope.resolveFunc(name.value, argTypes)

        val sym = resolveSingleFuncSym(funcNameExpr = name, overloads = costOverloads)
            ?: return ErrorType

        val paramList = sym.params.list

        for (i in paramList.indices) {
            val param = paramList[i]
            val argType = argTypes.getOrNull(i)

            if (argType == null) {
                val msg = Messages.F_NO_VALUE_PASSED_FOR_PARAMETER
                    .format(param.name, param.type)
                name.error(msg)
                continue
            }

            if (!argType.canCastTo(param.type)) {
                val msg = Messages.F_ARGUMENT_TYPE_MISMATCH
                    .format(param.type, argType)
                argNodes.getOrNull(i)?.error(msg)
                continue
            }
        }

        target bind sym
        return sym.returnType.also { target attach it }
    }

    private fun resolve(target: BaseDatatypeNode): Type {
        return when (target) {
            is DatatypeNode -> resolve(target)
            is ScopedDatatypeNode -> resolve(target)

            is AutoDatatypeNode,
            is VoidDatatypeNode -> PrimitiveTypes.void

            is FuncDatatypeNode -> resolve(target)
            is ErrorDatatypeNode -> ErrorType
            else -> ErrorType
        }.also { target attach it }
    }

    private fun resolve(target: FuncDatatypeNode): Type {
        return FuncType(
            paramTypes = target.paramDatatypes.map { resolve(it) },
            returnType = resolve(target.returnDatatype)
        ).applyTypeModifiers(
            pointerLevel = target.ptrLvl,
            isConst = target.isConst,
            isReference = target.isReference
        ).also { target attach it }
    }

    private fun resolveIdentifierWithSym(target: IdentifierNode, sym: Symbol): Type {
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

            is TypeSymbol -> createUserType(sym = sym)

            is FuncSymbol -> sym.toFuncType()

            is OverloadedFuncSymbol -> {
                OverloadedFuncType(
                    name = sym.name,
                    overloads = sym.overloads
                )
            }

            else -> target.error(::symNotDefinedError)
        }.also { target attach it }
    }

    private fun resolve(target: IdentifierNode, asMember: Boolean = false): Type {
        val sym = scope.resolve(name = target.value, asMember = asMember)
            ?: return target.error(::symNotDefinedInError)

        return resolveIdentifierWithSym(target, sym)
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

        if (!leftType.isExprType) {
            target.left.error(Messages.EXPECTED_A_VALUE)
            leftType = ErrorType
        }

        if (!rightType.isExprType) {
            target.right.error(Messages.EXPECTED_A_VALUE)
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

        return resolveOperFunc(target, leftType, rightType, operator, argTypes)
    }

    private fun resolveOperFunc(
        target: ExprNode,
        leftType: Type,
        rightType: Type,
        operator: OperatorType,
        argTypes: List<Type>
    ): Type {
        val symbols = scope.resolveOperatorFunc(operator = operator, argTypes = argTypes)
//            ?.attachSymbol(target)

        if (symbols.isNullOrEmpty())
            return target.error(
                Messages.F_NO_BIN_OPER_FUNC_OVERLOAD
                    .format(operator.symbol, leftType, rightType)
            )

        if (symbols.size > 1)
            return target.error(Messages.AMBIGUOUS_OVERLOADED_OPERATOR)

        val operFunc = symbols[0]

        val isConst = operFunc.returnType.isConst

        return operFunc.returnType.setFlags(
            isConst = isConst,
            isExprType = true,
            isLvalue = false,
            isMutable = false
        ).also { target attach it }
    }

    private fun resolveIs(
        target: BinOpNode,
        leftType: Type,
        rightType: Type
    ): Type {
        if (!leftType.isExprType)
            target.left.error(Messages.EXPECTED_AN_EXPRESSION)

        if (rightType.isExprType)
            target.right.error(Messages.EXPECTED_TYPE_NAME)

        return PrimitiveTypes.bool.setFlags(
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
                target.right.error(Messages.EXPECTED_TYPE_NAME)
                ErrorType
            }

            leftType is OverloadedFuncType -> {
                resolveForType(target.left, rightType)
            }

            leftType.canCastTo(rightType) -> rightType

            else -> {
                if (!leftType.canCastTo(rightType)) {
                    val msg = Messages.F_CANNOT_CAST_TYPE
                        .format(leftType, rightType)

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
                target.left.error(Messages.EXPECTED_VARIABLE)

            leftType.isConst ->
                target.error(Messages.ASSIGNMENT_TO_CONSTANT_VARIABLE)

            !leftType.isMutable ->
                target.error(Messages.ASSIGNMENT_TO_IMMUTABLE_VARIABLE)
        }

        if (!rightType.canCastTo(leftType)) {
            target.error(
                Messages.F_TYPE_MISMATCH.format(leftType, rightType)
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

    private fun resolve(target: DatatypeNode): Type {
        val name = target.identifier
        val sym = scope.resolve(name.value)
        target bind sym

        val pointerLevel = target.ptrLvl

        val type = when (sym) {
            is PrimitiveTypeSymbol -> sym.type

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
                return ErrorType
            }
        }

        return type.applyTypeModifiers(
            pointerLevel = pointerLevel,
            isConst = target.isConst || type.isConst,
            isReference = target.isReference
        )
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
                    it.error(Messages.EXPECTED_CONST_VALUE)
                    TemplateArg.ArgType(ErrorType)
                }
            }

            return@map TemplateArg.ArgType(type = type)
        }
    }


    /*private fun resolve(target: UserTypeSymbol): Type {
        when (target) {

        }
    }*/

    private fun resolve(target: UnaryOpNode): Type {
        return ErrorType
    }

    private fun resolve(target: LiteralNode<*>): Type {
        val type = when (target) {
            is LiteralNode.BooleanLiteral -> PrimitiveTypes.boolConst
            is LiteralNode.CharLiteral -> PrimitiveTypes.charConst
            is LiteralNode.DoubleLiteral -> PrimitiveTypes.float64Const
            is LiteralNode.FloatLiteral -> PrimitiveTypes.float32Const
            is LiteralNode.IntLiteral -> PrimitiveTypes.int32Const
            is LiteralNode.LongLiteral -> PrimitiveTypes.int64Const
            is LiteralNode.StringLiteral -> PrimitiveTypes.constCharPtr
            is LiteralNode.UIntLiteral -> PrimitiveTypes.uint32Const
            is LiteralNode.ULongLiteral -> PrimitiveTypes.uint64Const
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
            val msg = Messages.F_NONE_OF_N_CANDIDATES_APPLICABLE_FOR_TYPE
                .format(overloads.size, type)

            target.error(msg)
            return null
        }

        return bestFunc
    }

    fun resolveForType(target: ExprNode, type: Type): Type {
        val initializerType = analyzer.typeResolver.resolve(target)

        if (initializerType is OverloadedFuncType) {
            if (type !is FuncType) {
                return target.error(Messages.AMBIGUOUS_OVERLOADED_FUNCTION)
            }

            val bestFuncSym = resolveBestOverloadForType(target, type, initializerType.overloads)

            target bind bestFuncSym

            return bestFuncSym?.toFuncType() ?: ErrorType
        }

        if (initializerType != ErrorType && !initializerType.canCastTo(type))
            target.error(
                Messages.F_TYPE_MISMATCH.format(type, initializerType)
            )

        return type
    }
}