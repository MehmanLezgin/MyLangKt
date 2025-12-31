package lang.semantics.resolvers

import lang.messages.Messages
import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.symbols.*
import lang.semantics.types.*
import lang.tokens.OperatorType

@OptIn(ExperimentalUnsignedTypes::class)
class TypeResolver(
    override val ctx: ISemanticAnalyzer
) : BaseResolver<ExprNode, Type>(ctx = ctx) {
    override fun resolve(target: ExprNode): Type {
        return when (target) {
            is LiteralNode<*> -> resolve(target)
            is NullLiteralNode -> BuiltInTypes.voidPtr
            is BaseDatatypeNode -> resolve(target)
            is IdentifierNode -> resolve(target)
            is BinOpNode -> resolve(target)
            is UnaryOpNode -> resolve(target)
            else -> ErrorType
        }.attachType(target)
    }

    private fun resolve(target: BaseDatatypeNode): Type {
        return when (target) {
            is DatatypeNode -> resolve(target)
            is VoidDatatypeNode -> BuiltInTypes.void
            is FuncDatatypeNode -> resolve(target)
            is ErrorDatatypeNode -> ErrorType
            else -> ErrorType
        }.attachType(target)
    }

    private fun resolve(target: FuncDatatypeNode): Type {
        return FuncType(
            paramTypes = target.paramDatatypes.map { resolve(it) },
            returnType = resolve(target.returnDatatype)
        ).applyTypeModifiers(
            pointerLevel = target.ptrLvl,
            isConst = target.isConst,
            isReference = target.isReference
        ).attachType(target)
    }

    private fun resolve(target: IdentifierNode): Type {
        val sym = scope.resolve(target.value)?.attachSymbol(target)

        if (sym == null) {
            target.error(::symNotDefinedError)
            return ErrorType
        }

        return when (sym) {
            is VarSymbol -> sym.type.setFlags(
                isMutable = sym.isMutable,
                isExprType = true,
                isLvalue = true
            )

            is ConstVarSymbol -> sym.type.setFlags(
                isExprType = true,
                isLvalue = true
            )

            is PrimitiveTypeSymbol -> sym.type

            is UserTypeSymbol -> {
                createUserType(sym = sym)
            }

            is FuncSymbol -> sym.toFuncType()

            is OverloadedFuncSymbol -> {
//                target.error(Messages.AMBIGUOUS_OVERLOADED_FUNCTION)
                OverloadedFuncType(
                    name = sym.name,
                    overloads = sym.overloads
                )
            }

            else -> {
                target.error(::symNotDefinedError)
                ErrorType
            }
        }.attachType(target)
    }

    private fun createUserType(
        sym: Symbol,
        templateArgs: List<TemplateArg> = emptyList()
    ) =
        UserType(
            name = sym.name,
            templateArgs = templateArgs,
            declaration = sym,
            flags = TypeFlags(isExprType = false)
        )

    private fun ExprNode.constFoldAndAttach(): ConstValueSymbol? {
        val constValue = ctx.constResolver.resolve(this)
        return if (constValue != null) {
            ConstValueSymbol(
                type = constValue.type,
                value = constValue
            ).attachSymbol(this)
        } else null
    }

    private fun resolve(target: BinOpNode): Type {
        target.constFoldAndAttach()?.let {
            return it.type
        }

        val leftType = resolve(target.left)
        val rightType = resolve(target.right)

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
        val operFunc = scope.resolveOperatorFunc(operator = operator, argTypes = argTypes)
            ?.attachSymbol(target)

        if (operFunc == null) {
            val msg = Messages.NO_BIN_OPER_FUNC_OVERLOAD
                .format(operator.symbol, leftType, rightType)

            semanticError(msg, target.pos)
            return ErrorType
        }

        val isConst = operFunc.returnType.isConst

        return operFunc.returnType.setFlags(
            isConst = isConst,
            isExprType = true,
            isLvalue = false,
            isMutable = false
        ).attachType(target)
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

        return BuiltInTypes.bool.setFlags(
            isConst = false,
            isExprType = true
        ).attachType(target)
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
                val msg = Messages.CANNOT_CAST_TYPE
                    .format(leftType, rightType)

                target.error(msg)
                rightType
            }
        }

        type.attachType(target)
            .attachType(target.left)

        return type.setFlags(
            isExprType = true,
            isConst = false,
            isLvalue = false,
            isMutable = false
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
                Messages.TYPE_MISMATCH.format(leftType, rightType)
            )
        }

        return leftType.setFlags(
            isExprType = true,
            isLvalue = true,
            isConst = leftType.isConst,
            isMutable = leftType.isMutable
        ).attachType(target)
    }

    private fun resolve(target: DatatypeNode): Type {
        val name = target.identifier
        val sym = scope.resolve(name.value)?.attachSymbol(target)
        val pointerLevel = target.ptrLvl

        val type = when (sym) {
            is PrimitiveTypeSymbol -> sym.type
            is UserTypeSymbol -> {
                createUserType(
                    sym = sym,
                    templateArgs = resolveTemplateArgs(target.typeNames)
                )
            }

            else -> {
                name.error(::symNotDefinedError)
                return ErrorType
            }
        }

        return type.applyTypeModifiers(
            pointerLevel = pointerLevel,
            isConst = target.isConst,
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
                val constValue = ctx.constResolver.resolve(target = it)
                return@map if (constValue != null)
                    TemplateArg.ArgConstValue(value = constValue)
                else {
                    it.error(Messages.EXPECTED_CONST_VALUE)
                    TemplateArg.ArgType(ErrorType)
                }
            }

            return@map TemplateArg.ArgType(type = type)

//            if (it is IdentifierNode) {
//                val sym = ctx.scope.resolve(it.value)
//                TemplateArg.ArgType(sym)
//            }

//            it.error(Messages.EXPECTED_CONST_VALUE)
//            TemplateArg.ArgType(ErrorType)
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
        val type = when (target.value) {
            is Boolean -> BuiltInTypes.boolConst
            is Byte -> BuiltInTypes.int8Const
            is UByte -> BuiltInTypes.uint8Const
            is Short -> BuiltInTypes.int16Const
            is UShort -> BuiltInTypes.uint16Const
            is Int -> BuiltInTypes.int32Const
            is UInt -> BuiltInTypes.uint32Const
            is Long -> BuiltInTypes.int64Const
            is ULong -> BuiltInTypes.uint64Const
            is Float -> BuiltInTypes.float32Const
            is Double -> BuiltInTypes.float64Const
            is Char -> BuiltInTypes.charConst
            else -> {
                semanticError(Messages.INVALID_LITERAL_VALUE, target.pos)
                return ErrorType
            }
        }.setFlags(isExprType = true, isConst = true)

        target.constFoldAndAttach()

        return type
    }

    fun resolveForType(target: ExprNode, type: Type): Type {
        val initializerType = ctx.typeResolver.resolve(target)

        var resultType = type

        if (initializerType is OverloadedFuncType) {
            if (type !is FuncType) {
                target.error(Messages.AMBIGUOUS_OVERLOADED_FUNCTION)
                return ErrorType
            }

            val bestFunc = scope.resolveExactOverload(
                overloads = initializerType.overloads,
                types = type.paramTypes,
                returnType = type.returnType
            )

            if (bestFunc == null) {
                val msg = Messages.NONE_OF_N_OVERLOADS_APPLICABLE_FOR_TYPE
                    .format(initializerType.overloads.size, type)
                target.error(msg)
                return ErrorType
            }

            resultType = bestFunc.toFuncType()
            bestFunc.attachSymbol(target)
        } else if (!initializerType.canCastTo(type)) {
            target.error(
                Messages.TYPE_MISMATCH.format(type, initializerType)
            )
        }

        return resultType
    }
}