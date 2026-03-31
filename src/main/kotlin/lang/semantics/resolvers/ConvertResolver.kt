package lang.semantics.resolvers

import lang.nodes.ExprNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.builtin.PrimitivesScope
import lang.semantics.scopes.ScopeResult
import lang.semantics.symbols.ConstructorSymbol
import lang.semantics.types.*

class ConvertResolver(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<ExprNode, ConversionInfo>(analyzer) {

    override fun resolve(target: ExprNode) = ConversionInfo.None

    private fun findConversionConstructor(fromType: Type, toType: Type): ConstructorSymbol? {
        val decl = toType.declaration ?: return null

        val typeScope = decl.staticScope.instanceScope

        val fromScope = scope
        val funcSym = analyzer.withScope(typeScope) {
            analyzer.overloadResolver.resolveConstructor(
                from = fromScope,
                argTypes = listOf(fromType),
                onlyImplicit = true
            )
        }

        val constructorSym = if (funcSym is ScopeResult.Success<*>)
            funcSym.sym as? ConstructorSymbol
        else null

        constructorSym ?: return null

        val param = constructorSym.params.list.getOrNull(0)
            ?: return null

        if (param.type == toType) // prevent cycle
            return null

        if (convert(fromType, param.type).notExists())
            return null

        return constructorSym
    }

    private fun cast(fromType: Type, toType: Type) =
        ConversionInfo.Cast(fromType, toType)

    private fun Type.castVoidPointer(toType: Type) =
        when (toType) {
            is PointerType,
            is FuncType -> cast(this, toType)

            else -> ConversionInfo.None
        }

    private fun PointerType.castPointer(toType: Type) =
        when {
            toType.isVoidPtr() || toType is PointerType && this.level == toType.level
                -> cast(this, toType)

            else -> ConversionInfo.None
        }


    private fun MethodType.castMethodType(toType: Type) =
        when {
            toType is MethodType &&
            this.ownerType == toType.ownerType
                -> this.castFuncType(toType)

            else -> ConversionInfo.None
        }

    private fun FuncType.castFuncType(toType: Type) =
        when {
            this is MethodType && toType !is MethodType ||
                    this !is MethodType && toType is MethodType
                -> ConversionInfo.None


            toType.isVoidPtr() -> cast(this, toType)

            toType !is FuncType ||
                    this.returnType != toType.returnType ||
                    this.paramTypes != toType.paramTypes

                -> ConversionInfo.None

            else -> cast(this, toType)
        }

    private fun Type.constructorConvert(toType: Type): ConversionInfo {
        val constructor = findConversionConstructor(fromType = this, toType = toType)
            ?: return ConversionInfo.None

        return when {
            this is PrimitiveType && toType is PrimitiveType ->
                ConversionInfo.Primitive(this, toType, constructor)

            else -> ConversionInfo.Constructor(this, toType, constructor)
        }
    }

    private fun classify(fromType: Type, toType: Type): ConversionKind {
        return when {
            fromType == toType ->
                ConversionKind.IDENTITY

            toType is UserType -> ConversionKind.CONSTRUCTOR

            fromType.isVoidPtr() ->
                ConversionKind.VOID_PTR

            fromType == PrimitivesScope.void ||
                    toType == PrimitivesScope.void ||
                    !fromType.isConst && toType.isConst ->

                ConversionKind.INVALID

            fromType is PointerType && toType is PointerType ->
                ConversionKind.POINTER

            fromType is PointerType && toType.isVoidPtr() ->
                ConversionKind.VOID_PTR

            fromType is MethodType -> ConversionKind.METHOD
            fromType is FuncType -> ConversionKind.FUNCTION

            fromType is PrimitiveType && toType is PrimitiveType ->
                ConversionKind.PRIMITIVE

            else ->
                ConversionKind.CONSTRUCTOR

        }
    }


    fun conversionCost(fromType: Type, toType: Type): Int {
        val conversionKind = classify(fromType, toType)
        return when (conversionKind) {
            ConversionKind.INVALID -> ConversionInfo.COST_NONE
            ConversionKind.IDENTITY -> ConversionInfo.COST_IDENTITY

            ConversionKind.CONSTRUCTOR -> ConversionInfo.COST_CONSTRUCTOR
            ConversionKind.VOID_PTR,
            ConversionKind.POINTER,
            ConversionKind.METHOD,
            ConversionKind.FUNCTION -> ConversionInfo.COST_CAST

            ConversionKind.PRIMITIVE ->
                primitiveConversionCost(
                    from = fromType as PrimitiveType,
                    to = toType as PrimitiveType
                )
        }
    }

    private fun primitiveConversionCost(from: PrimitiveType, to: PrimitiveType): Int {
        if (from == to) return ConversionInfo.COST_IDENTITY

        val familyCost = when (from.family) {
            to.family -> 0
            PrimitiveFamily.BOOL -> 1
            PrimitiveFamily.CHAR -> 2
            PrimitiveFamily.INT if to.family == PrimitiveFamily.FLOAT -> 3
            else -> 4
        }

        val sizeCost = kotlin.math.abs(
            from.primitiveSize.bytes - to.primitiveSize.bytes
        )

        val signedCost =
            if (from.signed != to.signed) 1 else 0

        return familyCost * 10 + sizeCost + signedCost
    }

    fun convert(fromType: Type, toType: Type): ConversionInfo {
        val conversionKind = classify(fromType, toType)

        return when (conversionKind) {
            ConversionKind.IDENTITY ->
                ConversionInfo.Identity(fromType)

            ConversionKind.VOID_PTR ->
                fromType.castVoidPointer(toType)

            ConversionKind.INVALID ->
                ConversionInfo.None

            ConversionKind.POINTER ->
                (fromType as PointerType).castPointer(toType)

            ConversionKind.METHOD ->
                (fromType as MethodType).castMethodType(toType)

            ConversionKind.FUNCTION ->
                (fromType as FuncType).castFuncType(toType)

            ConversionKind.PRIMITIVE,
            ConversionKind.CONSTRUCTOR ->
                fromType.constructorConvert(toType)

        }
    }
}