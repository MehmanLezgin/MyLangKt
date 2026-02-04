package lang.semantics.resolvers

import lang.messages.Messages
import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.builtin.PrimitivesScope
import lang.semantics.scopes.*
import lang.semantics.symbols.*
import lang.semantics.types.ConstValue
import lang.semantics.types.ErrorType
import lang.semantics.types.OverloadedFuncType
import lang.semantics.types.Type
import lang.tokens.KeywordType
import lang.tokens.Pos
import kotlin.reflect.KClass

class DeclarationResolver(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<DeclStmtNode, Unit>(analyzer = analyzer) {
    override fun resolve(target: DeclStmtNode) {
        when (target) {
            is VarDeclStmtNode -> resolve(target)
            is ConstructorDeclStmtNode -> resolve(target)
            is DestructorDeclStmtNode -> resolve(target)
            is FuncDeclStmtNode -> resolve(target)
            is InterfaceDeclStmtNode -> resolve(target)
            is ClassDeclStmtNode -> resolve(target)
            is EnumDeclStmtNode -> resolve(target)
            is TypedefStmtNode -> resolve(target)
            is NamespaceStmtNode -> resolve(target)
        }
    }

    private fun <T : Symbol> T.bindAndExport(node: ExprNode, isExport: Boolean): T {
        bind(node)
        exportIfNeeded(isExport)
        return this
    }

    private fun <T : Symbol> T.bind(node: ExprNode): T {
        node bind this
        return this
    }

    private fun <T : Symbol> T.exportIfNeeded(isExport: Boolean): T {
        if (isExport)
            analyzer.exportSymbol(this)
        return this
    }

    private fun resolve(node: NamespaceStmtNode) {
        val modifiers = resolveNamespaceModifiers(node.modifiers)

        val sym = scope.defineNamespace(node, isExport = modifiers.isExport)
            .bindAndExport(node, modifiers.isExport)

        analyzer.withScopeResolveBody(targetScope = sym.scope, body = node.body)
    }

    private fun resolve(node: TypedefStmtNode) {
        val modifiers = resolveTypedefModifiers(node.modifiers)

        val type = analyzer.typeResolver.resolve(node.dataType)
        node attach type
        if (type is ErrorType) return
        scope.defineTypedef(node, type)
            .bindAndExport(node, modifiers.isExport)
    }

    private fun resolveAutoVarType(node: VarDeclStmtNode): Type {
        if (node.initializer == null) {
            semanticError(Messages.EXPECTED_TYPE_NAME, node.name.pos)
            return ErrorType
        }

        val initType = analyzer.typeResolver.resolve(node.initializer)

        val sym = node.initializer.getResolvedSymbol()

        when {
            sym is ConstValueSymbol ->
                return initType.setFlags(isConst = false)

            initType is OverloadedFuncType ->
                node.initializer.error(Messages.AMBIGUOUS_OVERLOADED_FUNCTION)

            else -> return initType
        }

        return ErrorType
    }

    private fun resolveVarType(node: VarDeclStmtNode): Type {
        if (node.dataType is AutoDatatypeNode) {
            val type = resolveAutoVarType(node)
            return type
        }

        var type = analyzer.typeResolver.resolve(node.dataType)

        if (type.isExprType) {
            type = ErrorType
            node.dataType.error(Messages.EXPECTED_TYPE_NAME)
        }

        if (node.initializer == null)
            return type

        return analyzer.typeResolver
            .resolveForType(node.initializer, type)
            .also { node.initializer attach it }
    }

    private fun resolveConstVar(node: VarDeclStmtNode, type: Type, modifiers: Modifiers) {
        var constValue: ConstValue<*>? = null

        if (type.isConst) {
            val sym = node.initializer?.getResolvedSymbol()

            if (sym is ConstValueSymbol) {
                var value = sym.value

                if (value != null && value.type != type)
                    value = analyzer.constResolver.resolveCast(value, type)

                constValue = value
            }
        }

//        if (constValue == null) return
        withEffectiveScope(modifiers.isStatic) {
            scope.defineConstVar(node, type, constValue, modifiers)
                .bindAndExport(node, modifiers.isExport)
        }
    }

    private fun resolve(node: VarDeclStmtNode) {
        val modifiers = resolveVarModifiers(node.modifiers)

        val type = resolveVarType(node)
        node attach type

        val isConst = type.isConst// || modifiers.isConst

        if (isConst && (!modifiers.isStatic && (scope is BaseTypeScope && scope !is NamespaceScope))) {
            node.error(Messages.CONST_VAR_MUST_BE_STATIC)
        }

        if (!isConst) {
            withEffectiveScope(modifiers.isStatic) {
                scope.defineVar(node, type, modifiers)
                    .bindAndExport(node, modifiers.isExport)
            }

            return
        }

        resolveConstVar(node, type, modifiers)
    }

    private fun <T> withEffectiveScope(isStatic: Boolean, block: () -> T): T {
        return if (!isStatic && scope is BaseTypeScope && scope !is NamespaceScope)
            analyzer.withScope((scope as BaseTypeScope).instanceScope, block)
        else
            block()
    }

    private fun resolveFuncParam(node: VarDeclStmtNode) {
        val paramsScope = scope

        if (paramsScope !is FuncParamsScope) {
            semanticError(Messages.CANNOT_RESOLVE_PARAM_OUTSIDE_FUNC, node.pos)
            return
        }

        val type = analyzer.typeResolver.resolve(node.dataType)

        paramsScope.defineParam(node, type)
            .also { node bind it }

        if (node.dataType is AutoDatatypeNode)
            semanticError(Messages.EXPECTED_TYPE_NAME, node.name.pos)
        else if (type == PrimitivesScope.void)
            semanticError(Messages.VOID_CANNOT_BE_PARAM_TYPE, node.name.pos)

        analyzer.typeResolver.resolve(node.dataType)
    }

    private fun resolve(node: FuncDeclStmtNode) {
        val paramsScope = FuncParamsScope(
            parent = scope,
            errorHandler = errorHandler
        )

        analyzer.enterScope(paramsScope)
        node.params
            .forEach { decl ->
                resolveFuncParam(node = decl)
            }

        val params = paramsScope.getParams()
        analyzer.exitScope()

        val returnType = analyzer.typeResolver.resolve(node.returnType)

        val modifiers = resolveFuncModifiers(node.modifiers)
        val funcSymbol = withEffectiveScope(modifiers.isStatic) {
            val pair = scope.defineFunc(node, params, returnType, modifiers)

                pair.second.exportIfNeeded(modifiers.isExport)
            pair.first.bind(node)
        }

        val funcScope = FuncScope(
            parent = scope,
            funcSymbol = funcSymbol,
            errorHandler = errorHandler
        )

        val funcType = funcSymbol.toFuncType()

        node attach funcType

        analyzer.withScopeResolveBody(targetScope = funcScope, body = node.body)
    }

    private fun resolve(node: ConstructorDeclStmtNode) {
        if (scope !is ClassScope)
            semanticError(Messages.CONSTRUCTOR_OUTSIDE_CLASS_ERROR, node.pos)

        resolve(node as FuncDeclStmtNode)
    }

    private fun resolve(node: DestructorDeclStmtNode) {
        if (scope !is ClassScope)
            semanticError(Messages.DESTRUCTOR_OUTSIDE_CLASS_ERROR, node.pos)

        resolve(node as FuncDeclStmtNode)
    }

    private fun resolve(node: InterfaceDeclStmtNode) {
        val modifiers = resolveInterfaceModifiers(node.modifiers)

        val superType = resolveSuperType(node.superInterface)

        if (superType != null && superType.declaration !is InterfaceSymbol) {
            semanticError(Messages.INTERFACE_CAN_EXTEND_INTERFACE, node.superInterface?.pos)
        }

        val sym = scope.defineInterface(node, modifiers, superType)
            .bindAndExport(node, modifiers.isExport)

        analyzer.withScopeResolveBody(targetScope = sym.scope, body = node.body)
    }

    private fun resolve(node: ClassDeclStmtNode) {
        val modifiers = resolveClassModifiers(node.modifiers)

        val superType = resolveSuperType(node.superClass)

        if (superType != null &&
            superType.declaration !is InterfaceSymbol &&
            superType.declaration !is ClassSymbol
        )
            semanticError(Messages.CLASS_CAN_EXTEND_INTERFACE_OR_CLASS, node.superClass?.pos)

        val sym = scope.defineClass(node, modifiers, superType)
            .bindAndExport(node, modifiers.isExport)

        analyzer.withScopeResolveBody(targetScope = sym.scope, body = node.body)
    }

    private fun resolveSuperType(
        superType: BaseDatatypeNode?
    ): Type? {
        if (superType == null || superType is VoidDatatypeNode) return null
        val type = analyzer.typeResolver.resolve(superType)

        if (type.declaration?.modifiers?.isOpen == false)
            semanticError(Messages.F_MUST_BE_OPEN_TYPE.format(type.declaration?.name), superType.pos)

        return type
    }

    private fun resolve(node: EnumDeclStmtNode) {
        val modifiers = resolveEnumModifiers(node.modifiers)
        val sym = scope.defineEnum(node, modifiers)
            .bindAndExport(node, modifiers.isExport)

        analyzer.withScopeResolveBody(targetScope = sym.scope, body = node.body)
    }

    fun modifierNodeToVisibility(modifierNode: ModifierNode): Visibility? {
        return when (modifierNode) {
            is ModifierNode.Public -> Visibility.PUBLIC
            is ModifierNode.Private -> Visibility.PRIVATE
            is ModifierNode.Protected -> Visibility.PROTECTED
            else -> null
        }
    }


    private fun resolveVisibility(modifiers: ModifierSetNode?): Visibility {
        if (modifiers == null || modifiers.nodes.isEmpty())
            return Visibility.PUBLIC

        var visibility: Visibility? = null
        var selectedVisibilityName = ""

        for (modifierNode in modifiers.nodes) {
            val v = modifierNodeToVisibility(modifierNode)
                ?: continue

            val name = modifierNode.keyword.value

            if (visibility == null) {
                visibility = v
                selectedVisibilityName = name
                continue
            }

            modifierNode.error(
                Messages.F_MODIFIER_IS_INCOMPATIBLE_WITH.format(name, selectedVisibilityName)
            )
        }

        return visibility ?: Visibility.PUBLIC
    }


    private fun resolveModifiers(
        node: ModifierSetNode?,
        illegalModifiers: Array<KClass<out ModifierNode>>,
        declKindName: String
    ): Modifiers {
        if (node == null) return Modifiers()

        val illegalSet = illegalModifiers.toSet()

        // 1. Репортим ошибки для запрещённых модификаторов
        illegalSet.forEach { cls ->
            node.get(cls)?.error(
                Messages.F_MODIFIER_IS_NOT_ALLOWED_ON
                    .format(node.get(cls)!!.keyword.value, declKindName)
            )
        }

        // 2. Хелпер: модификатор есть И он разрешён
        fun <T : ModifierNode> hasAllowed(cls: KClass<T>): Boolean {
            return cls !in illegalSet && node.get(cls) != null
        }

        // 3. Собираем флаги
        var isStatic = scope is NamespaceScope || hasAllowed(ModifierNode.Static::class)
        var isExport = hasAllowed(ModifierNode.Export::class)
        val isAbstract = hasAllowed(ModifierNode.Abstract::class)
        val isOpen = hasAllowed(ModifierNode.Open::class)
        val isOverride = hasAllowed(ModifierNode.Override::class)
        val visibility = resolveVisibility(modifiers = node)

        // 4. Контекстные проверки (scope-dependent)

        checkModifier(
            isExport && (scope !is NamespaceScope || !(scope as NamespaceScope).isExport),
            node.get(ModifierNode.Export::class)?.pos,
            Messages.EXPORT_IS_NOT_ALLOWED_IN_THIS_SCOPE
        ) {
            isExport = false
        }

        checkModifier(
            isStatic && (scope !is NamespaceScope || scope !is BaseTypeScope),
            node.get(ModifierNode.Static::class)?.pos,
            Messages.STATIC_IS_NOT_ALLOWED_IN_THIS_SCOPE
        ) {
            isStatic = false
        }

        // 5. Нормализация зависимых модификаторов
        val finalIsOpen = isOpen || isAbstract

        return Modifiers(
            isStatic = isStatic,
            isAbstract = isAbstract,
            isOpen = finalIsOpen,
            isOverride = isOverride,
            isExport = isExport,
            visibility = visibility
        )
    }

    private val namespaceIllegalModifiers = arrayOf(
        ModifierNode.Override::class,
        ModifierNode.Open::class,
        ModifierNode.Abstract::class
    )

    private val resolveTypedefModifiers = namespaceIllegalModifiers

    private val classIllegalModifiers = arrayOf(
        ModifierNode.Static::class,
        ModifierNode.Override::class
    )

    private val interfaceIllegalModifiers = arrayOf(
        ModifierNode.Open::class,
        ModifierNode.Static::class,
        ModifierNode.Override::class
    )

    private val enumIllegalModifiers = arrayOf(
        ModifierNode.Static::class,
        ModifierNode.Open::class,
        ModifierNode.Abstract::class,
        ModifierNode.Override::class
    )

    private val varIllegalModifiers = arrayOf(
        ModifierNode.Open::class,
        ModifierNode.Abstract::class,
        ModifierNode.Override::class
    )

    private val funcIllegalModifiers: Array<KClass<out ModifierNode>> = arrayOf(

    )

    private fun resolveNamespaceModifiers(node: ModifierSetNode?): Modifiers {
        return resolveModifiers(
            node = node,
            illegalModifiers = namespaceIllegalModifiers,
            declKindName = Messages.NAMESPACE
        )
    }

    private fun resolveTypedefModifiers(node: ModifierSetNode?): Modifiers {
        return resolveModifiers(
            node = node,
            illegalModifiers = resolveTypedefModifiers,
            declKindName = Messages.CLASS
        )
    }

    private fun resolveClassModifiers(node: ModifierSetNode?): Modifiers {
        return resolveModifiers(
            node = node,
            illegalModifiers = classIllegalModifiers,
            declKindName = Messages.CLASS
        )
    }

    private fun resolveInterfaceModifiers(node: ModifierSetNode?): Modifiers {
        return resolveModifiers(
            node = node,
            illegalModifiers = interfaceIllegalModifiers,
            declKindName = Messages.INTERFACE
        ).also { it.isAbstract = true }
    }

    private fun resolveEnumModifiers(node: ModifierSetNode?): Modifiers {
        val modifiers = resolveModifiers(
            node = node,
            illegalModifiers = enumIllegalModifiers,
            declKindName = Messages.ENUM
        )

        if (node == null) return modifiers

        return modifiers
    }

    private fun resolveVarModifiers(node: ModifierSetNode?): Modifiers {
        val modifiers = resolveModifiers(
            node = node,
            illegalModifiers = varIllegalModifiers,
            declKindName = Messages.VARIABLE
        )

        return modifiers
    }

    private inline fun checkModifier(
        flag: Boolean,
        pos: Pos?,
        errorMsg: String,
        action: () -> Unit
    ) {
        if (flag) {
            semanticError(errorMsg, pos)
            action()
        }
    }

    private fun resolveFuncModifiers(node: ModifierSetNode?): Modifiers {
        val modifiers = resolveModifiers(
            node = node,
            illegalModifiers = funcIllegalModifiers,
            declKindName = Messages.CLASS
        )

        if (node == null) return modifiers

        val staticPos = node.get(ModifierNode.Static::class)?.pos
        val overridePos = node.get(ModifierNode.Override::class)?.pos

        if (modifiers.isStatic) {
            checkModifier(
                modifiers.isAbstract,
                staticPos,
                Messages.STATIC_FUNC_CANNOT_BE_ABSTRACT
            ) { modifiers.isAbstract = false }

            checkModifier(
                modifiers.isOpen,
                staticPos,
                Messages.STATIC_FUNC_CANNOT_BE_OPEN
            ) { modifiers.isOpen = false }

            checkModifier(
                modifiers.isOverride,
                staticPos,
                Messages.STATIC_FUNC_CANNOT_BE_OVERRIDDEN
            ) { modifiers.isOverride = false }
        }

        if (modifiers.isOverride) {
            // scope check
            checkModifier(
                scope !is ClassScope,
                overridePos,
                Messages.OVERRIDE_ALLOWED_ONLY_IN_CLASS_SCOPE
            ) { modifiers.isOverride = false }

            // visibility check
            checkModifier(
                modifiers.visibility != Visibility.PUBLIC,
                overridePos,
                Messages.OVERRIDE_MEMBER_MUST_BE_PUBLIC
            ) { modifiers.isOverride = false }

            // incompatible with abstract
            checkModifier(
                modifiers.isAbstract, overridePos,
                Messages.F_MODIFIER_IS_INCOMPATIBLE_WITH.format(
                    KeywordType.ABSTRACT.value, KeywordType.OVERRIDE.value
                )
            ) { modifiers.isOverride = false }
        }

        return modifiers
    }
}