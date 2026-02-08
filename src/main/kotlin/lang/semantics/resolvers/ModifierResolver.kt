package lang.semantics.resolvers

import lang.core.KeywordType
import lang.core.SourceRange
import lang.messages.Msg
import lang.messages.Terms
import lang.nodes.ModifierNode
import lang.nodes.ModifierSetNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.scopes.BaseTypeScope
import lang.semantics.scopes.ClassScope
import lang.semantics.scopes.NamespaceScope
import lang.semantics.symbols.Modifiers
import lang.semantics.symbols.Visibility
import kotlin.collections.forEach
import kotlin.reflect.KClass

class ModifierResolver(analyzer: ISemanticAnalyzer) : BaseResolver<ModifierSetNode?, Modifiers>(analyzer) {
    private val namespaceAllowedModifiers = setOf(
        ModifierNode.Private::class,
        ModifierNode.Public::class,
        ModifierNode.Protected::class,
        ModifierNode.Export::class,
        ModifierNode.Static::class,
    )

    private val usingAllowedModifiers = namespaceAllowedModifiers

    private val classAllowedModifiers = setOf(
        ModifierNode.Private::class,
        ModifierNode.Public::class,
        ModifierNode.Protected::class,
        ModifierNode.Export::class,
        ModifierNode.Open::class,
        ModifierNode.Abstract::class,
    )

    private val interfaceAllowedModifiers = setOf(
        ModifierNode.Public::class,
        ModifierNode.Protected::class,
        ModifierNode.Export::class,
        ModifierNode.Abstract::class,
    )

    private val enumAllowedModifiers = setOf(
        ModifierNode.Public::class,
        ModifierNode.Protected::class,
        ModifierNode.Export::class,
    )

    private val varAllowedModifiers = setOf(
        ModifierNode.Private::class,
        ModifierNode.Public::class,
        ModifierNode.Protected::class,
        ModifierNode.Export::class,
        ModifierNode.Static::class,
    )

    private val funcAllowedModifiers = setOf(
        ModifierNode.Private::class,
        ModifierNode.Public::class,
        ModifierNode.Protected::class,
        ModifierNode.Export::class,
        ModifierNode.Static::class,
        ModifierNode.Override::class,
        ModifierNode.Open::class,
        ModifierNode.Abstract::class,
        ModifierNode.Infix::class
    )

    override fun resolve(target: ModifierSetNode?): Modifiers {
        return Modifiers()
    }

    private fun modifierNodeToVisibility(modifierNode: ModifierNode): Visibility? {
        return when (modifierNode) {
            is ModifierNode.Public -> Visibility.PUBLIC
            is ModifierNode.Private -> Visibility.PRIVATE
            is ModifierNode.Protected -> Visibility.PROTECTED
            else -> null
        }
    }

    fun resolveNamespaceModifiers(node: ModifierSetNode?): Modifiers {
        return resolveModifiers(
            node = node,
            allowedModifiers = namespaceAllowedModifiers,
            declKindName = Terms.NAMESPACE
        )
    }

    fun resolveUsingModifiers(node: ModifierSetNode?): Modifiers {
        return resolveModifiers(
            node = node,
            allowedModifiers = usingAllowedModifiers,
            declKindName = Terms.USING
        )
    }

    fun resolveClassModifiers(node: ModifierSetNode?): Modifiers {
        return resolveModifiers(
            node = node,
            allowedModifiers = classAllowedModifiers,
            declKindName = Terms.CLASS
        )
    }

    fun resolveInterfaceModifiers(node: ModifierSetNode?): Modifiers {
        return resolveModifiers(
            node = node,
            allowedModifiers = interfaceAllowedModifiers,
            declKindName = Terms.INTERFACE
        ).also { it.isAbstract = true }
    }

    fun resolveEnumModifiers(node: ModifierSetNode?): Modifiers {
        val modifiers = resolveModifiers(
            node = node,
            allowedModifiers = enumAllowedModifiers,
            declKindName = Terms.ENUM
        )

        if (node == null) return modifiers

        return modifiers
    }

    fun resolveVarModifiers(node: ModifierSetNode?): Modifiers {
        val modifiers = resolveModifiers(
            node = node,
            allowedModifiers = varAllowedModifiers,
            declKindName = Terms.VARIABLE
        )

        return modifiers
    }

    fun resolveFuncModifiers(node: ModifierSetNode?): Modifiers {
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

        allowedModifiers.forEach { cls ->
            val modifierNode = node.get(cls)
            if (modifierNode != null) return@forEach
            modifierNode?.error(
                Msg.F_MODIFIER_IS_NOT_ALLOWED_ON.format(
                    modifierNode.keyword.value, declKindName
                )
            )
        }


        var isStatic = scope is NamespaceScope || hasAllowed(ModifierNode.Static::class)
        var isExport = hasAllowed(ModifierNode.Export::class)
        val isAbstract = hasAllowed(ModifierNode.Abstract::class)
        val isOpen = hasAllowed(ModifierNode.Open::class)
        val isOverride = hasAllowed(ModifierNode.Override::class)
        val isInfix = hasAllowed(ModifierNode.Infix::class)
        val visibility = resolveVisibility(modifiers = node)

        checkModifier(
            isExport && (scope !is NamespaceScope || !(scope as NamespaceScope).isExport),
            node.get(ModifierNode.Export::class)?.range,
            Msg.EXPORT_IS_NOT_ALLOWED_IN_THIS_SCOPE
        ) { isExport = false }

        checkModifier(
            isStatic && (scope !is NamespaceScope && scope !is BaseTypeScope),
            node.get(ModifierNode.Static::class)?.range,
            Msg.STATIC_IS_NOT_ALLOWED_IN_THIS_SCOPE
        ) { isStatic = false }

        val finalIsOpen = isOpen || isAbstract

        return Modifiers(
            isStatic = isStatic,
            isAbstract = isAbstract,
            isOpen = finalIsOpen,
            isOverride = isOverride,
            isExport = isExport,
            isInfix = isInfix,
            visibility = visibility
        )
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