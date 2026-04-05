package lang.semantics.resolvers

import lang.infrastructure.KeywordType
import lang.infrastructure.SourceRange
import lang.messages.Msg
import lang.messages.Terms
import lang.nodes.ClassDeclStmtNode
import lang.nodes.EnumDeclStmtNode
import lang.nodes.FuncDeclStmtNode
import lang.nodes.InterfaceDeclStmtNode
import lang.nodes.ModifierNode
import lang.nodes.ModifierSetNode
import lang.nodes.ModuleStmtNode
import lang.nodes.TemplateStmtNode
import lang.nodes.UsingDirectiveNode
import lang.nodes.VarDeclStmtNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.scopes.BaseTypeScope
import lang.semantics.scopes.ClassScope
import lang.semantics.scopes.InterfaceScope
import lang.semantics.scopes.ModuleScope
import lang.semantics.symbols.Modifiers
import lang.semantics.symbols.Visibility
import kotlin.collections.forEach
import kotlin.let
import kotlin.reflect.KClass

class ModifierResolver(
    analyzer: ISemanticAnalyzer
) : BaseResolver<ModifierSetNode?, Modifiers>(analyzer) {
    private val modifiersCache = mutableMapOf<ModifierSetNode, Modifiers>()

    private fun checkCache(node: ModifierSetNode?): Modifiers? {
        if (node == null) return null
        val cached = modifiersCache[node] ?: return null
        modifiersCache[node] = cached
        return cached
    }

    private val moduleAllowedModifiers = setOf(
        ModifierNode.Private::class,
        ModifierNode.Public::class,
        ModifierNode.Internal::class,
        ModifierNode.Static::class,
    )

    private val usingAllowedModifiers = moduleAllowedModifiers

    private val classAllowedModifiers = setOf(
        ModifierNode.Private::class,
        ModifierNode.Public::class,
        ModifierNode.Internal::class,
        ModifierNode.Open::class,
        ModifierNode.Abstract::class,
    )

    private val interfaceAllowedModifiers = setOf(
        ModifierNode.Public::class,
        ModifierNode.Private::class,
        ModifierNode.Internal::class,
        ModifierNode.Abstract::class,
    )

    private val enumAllowedModifiers = setOf(
        ModifierNode.Public::class,
        ModifierNode.Private::class,
        ModifierNode.Internal::class
    )

    private val varAllowedModifiers = setOf(
        ModifierNode.Private::class,
        ModifierNode.Public::class,
        ModifierNode.Internal::class,
        ModifierNode.Static::class,
    )

    private val funcAllowedModifiers = setOf(
        ModifierNode.Private::class,
        ModifierNode.Public::class,
        ModifierNode.Internal::class,
        ModifierNode.Static::class,
        ModifierNode.Override::class,
        ModifierNode.Open::class,
        ModifierNode.Abstract::class,
        ModifierNode.Infix::class
    )

    private val constructorAllowedModifiers = setOf(
        ModifierNode.Private::class,
        ModifierNode.Public::class,
        ModifierNode.Internal::class,
        ModifierNode.Implicit::class,
    )

    private val destructorAllowedModifiers = setOf(
        ModifierNode.Private::class,
        ModifierNode.Public::class,
        ModifierNode.Internal::class,
    )

    override fun resolve(target: ModifierSetNode?): Modifiers {
        return Modifiers()
    }

    private fun modifierNodeToVisibility(modifierNode: ModifierNode): Visibility? {
        return when (modifierNode) {
            is ModifierNode.Public -> Visibility.PUBLIC
            is ModifierNode.Private -> Visibility.PRIVATE
            is ModifierNode.Internal -> Visibility.INTERNAL
            else -> null
        }
    }

    fun resolveTemplateModifiers(node: TemplateStmtNode): Modifiers {
        val modifiersNode = node.modifiers

        return when (node.declStmt) {
            is ClassDeclStmtNode -> resolveClassModifiers(node = modifiersNode)
            is EnumDeclStmtNode -> resolveEnumModifiers(node = modifiersNode)
            is FuncDeclStmtNode -> resolveFuncModifiers(node = modifiersNode)
            is InterfaceDeclStmtNode -> resolveInterfaceModifiers(node = modifiersNode)
            is ModuleStmtNode -> resolveModuleModifiers(node = modifiersNode)
            is VarDeclStmtNode -> resolveVarModifiers(node = modifiersNode)
        }
    }

    fun resolveModuleModifiers(node: ModifierSetNode?): Modifiers {
        checkCache(node)?.let { return it }

        return resolveModifiers(
            node = node,
            allowedModifiers = moduleAllowedModifiers,
            declKindName = Terms.NAMESPACE
        )
    }

    fun resolveUsingModifiers(node: ModifierSetNode?): Modifiers {
        checkCache(node)?.let { return it }

        return resolveModifiers(
            node = node,
            allowedModifiers = usingAllowedModifiers,
            declKindName = Terms.USING
        )
    }

    fun resolveClassModifiers(node: ModifierSetNode?): Modifiers {
        checkCache(node)?.let { return it }

        return resolveModifiers(
            node = node,
            allowedModifiers = classAllowedModifiers,
            declKindName = Terms.CLASS
        )
    }

    fun resolveInterfaceModifiers(node: ModifierSetNode?): Modifiers {
        checkCache(node)?.let { return it }

        return resolveModifiers(
            node = node,
            allowedModifiers = interfaceAllowedModifiers,
            declKindName = Terms.INTERFACE
        ).also { it.isAbstract = true }
    }

    fun resolveEnumModifiers(node: ModifierSetNode?): Modifiers {
        checkCache(node)?.let { return it }

        val modifiers = resolveModifiers(
            node = node,
            allowedModifiers = enumAllowedModifiers,
            declKindName = Terms.ENUM
        )

        if (node == null) return modifiers

        return modifiers
    }

    fun resolveVarModifiers(node: ModifierSetNode?): Modifiers {
        checkCache(node)?.let { return it }

        val modifiers = resolveModifiers(
            node = node,
            allowedModifiers = varAllowedModifiers,
            declKindName = Terms.VARIABLE
        )

        return modifiers
    }

    fun resolveConstructorModifiers(node: ModifierSetNode?): Modifiers {
        checkCache(node)?.let { return it }

        val modifiers = resolveModifiers(
            node = node,
            allowedModifiers = constructorAllowedModifiers,
            declKindName = Terms.CONSTRUCTOR
        )

        return modifiers
    }

    fun resolveDestructorModifiers(node: ModifierSetNode?): Modifiers {
        checkCache(node)?.let { return it }

        val modifiers = resolveModifiers(
            node = node,
            allowedModifiers = destructorAllowedModifiers,
            declKindName = Terms.DESTRUCTOR
        )

        return modifiers
    }

    fun resolveFuncModifiers(node: ModifierSetNode?): Modifiers {
        checkCache(node)?.let { return it }

        val modifiers = resolveModifiers(
            node = node,
            allowedModifiers = funcAllowedModifiers,
            declKindName = Terms.FUNCTION
        )

        if (node == null) return modifiers

        val staticPos = node.get(ModifierNode.Static::class)?.range
        val overridePos = node.get(ModifierNode.Override::class)?.range

        if (modifiers.isStatic) {
            checkModifier(
                modifiers.isAbstract,
                staticPos,
                Msg.STATIC_FUNC_CANNOT_BE_ABSTRACT
            ) { modifiers.isAbstract = false }

            checkModifier(
                modifiers.isOpen,
                staticPos,
                Msg.STATIC_FUNC_CANNOT_BE_OPEN
            ) { modifiers.isOpen = false }

            checkModifier(
                modifiers.isOverride,
                staticPos,
                Msg.STATIC_FUNC_CANNOT_BE_OVERRIDDEN
            ) { modifiers.isOverride = false }
        }

        if (modifiers.isOverride) {
            // scope check
            checkModifier(
                scope !is ClassScope,
                overridePos,
                Msg.OVERRIDE_ALLOWED_ONLY_IN_CLASS_SCOPE
            ) { modifiers.isOverride = false }

            // visibility check
            checkModifier(
                modifiers.visibility != Visibility.PUBLIC,
                overridePos,
                Msg.OVERRIDE_MEMBER_MUST_BE_PUBLIC
            ) { modifiers.isOverride = false }

            // incompatible with abstract
            checkModifier(
                modifiers.isAbstract, overridePos,
                Msg.F_MODIFIER_IS_INCOMPATIBLE_WITH.format(
                    KeywordType.ABSTRACT.value, KeywordType.OVERRIDE.value
                )
            ) { modifiers.isOverride = false }
        }

        return modifiers
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
                Msg.F_MODIFIER_IS_INCOMPATIBLE_WITH.format(name, selectedVisibilityName)
            )
        }

        return visibility ?: Visibility.PUBLIC
    }

    private fun resolveModifiers(
        node: ModifierSetNode?,
        allowedModifiers: Set<KClass<out ModifierNode>>,
        declKindName: String
    ): Modifiers {
        if (node == null) return Modifiers()

        fun <T : ModifierNode> hasAllowed(cls: KClass<T>): Boolean {
            return cls in allowedModifiers && node.get(cls) != null
        }

        node.nodes.forEach { modifierNode ->
            if (allowedModifiers.any { it.isInstance(modifierNode) })
                return@forEach

            modifierNode.error(
                Msg.F_MODIFIER_IS_NOT_ALLOWED_ON.format(
                    modifierNode.keyword.value, declKindName
                )
            )
        }

        var isStatic = scope is ModuleScope || hasAllowed(ModifierNode.Static::class)
        val isAbstract = hasAllowed(ModifierNode.Abstract::class)
        val isOpen = hasAllowed(ModifierNode.Open::class)
        val isOverride = hasAllowed(ModifierNode.Override::class)
        val isInfix = hasAllowed(ModifierNode.Infix::class)
        val isImplicit = hasAllowed(ModifierNode.Implicit::class)
        var visibility = resolveVisibility(modifiers = node)

        checkModifier(
            isStatic && (scope !is ModuleScope && scope !is BaseTypeScope),
            node.get(ModifierNode.Static::class)?.range,
            Msg.F_MODIFIER_IS_NOT_ALLOWED_IN_THIS_SCOPE.format(
                KeywordType.STATIC.value, declKindName
            )
        ) { isStatic = false }

        checkModifier(
            visibility == Visibility.INTERNAL && (scope !is ClassScope && scope !is InterfaceScope),
            node.get(ModifierNode.Internal::class)?.range,
            Msg.F_MODIFIER_IS_NOT_ALLOWED_IN_THIS_SCOPE.format(
                KeywordType.INTERNAL.value, declKindName
            )
        ) { visibility = Visibility.PUBLIC }

        val finalIsOpen = isOpen || isAbstract

        return Modifiers(
            isStatic = isStatic,
            isAbstract = isAbstract,
            isOpen = finalIsOpen,
            isOverride = isOverride,
            isInfix = isInfix,
            isImplicit = isImplicit,
            visibility = visibility
        ).also {
            modifiersCache[node] = it
        }
    }

    private inline fun checkModifier(
        flag: Boolean,
        range: SourceRange?,
        errorMsg: String,
        action: () -> Unit
    ) {
        if (flag) {
            semanticError(errorMsg, range)
            action()
        }
    }
}