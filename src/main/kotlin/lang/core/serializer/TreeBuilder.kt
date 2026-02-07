package lang.core.serializer

import kotlin.reflect.KClass

typealias ChildrenMapRaw = Map<String, Any?>
typealias ChildrenMap = Map<String, Tree>

sealed class Tree {
    data class Node(val value: Any) : Tree()
    data class ItemList(val list: List<*>) : Tree()
    data class Str(val str: String) : Tree()
}

class TreeBuilder<T : Any>(
    private val klass: KClass<T>,
    private val childrenProvider: (T, String, String) -> ChildrenMapRaw,
    private val nameProvider: (Any) -> String = { it::class.simpleName ?: "?" }
) {
    private val sb = StringBuilder(256)

    private fun branch(last: Boolean) = if (last) "└── " else "├── "
    private fun indent(last: Boolean) = if (last) "    " else "│   "

    private fun listChildren(list: Collection<*>): ChildrenMap =
        list.mapIndexed { i, v ->
            "[${i + 1}]" to when (v) {
                null -> Tree.Str("null")
                is Collection<*> -> Tree.ItemList(v.toList())
                else -> Tree.Node(v)
            }
        }.toMap()

    private fun buildChildren(children: ChildrenMap, baseIndent: String) {
        if (children.isEmpty()) {
            return
        }
        val keys = children.keys

        keys.forEachIndexed { i, key ->
            val last = i == keys.size - 1
            val tree = children[key]!!

            sb.append('\n')
                .append(baseIndent)
                .append(branch(last))
                .append(key)
                .append(":")


            build(tree, baseIndent + indent(last), last)

            if (last)
                sb.append('\n').append(baseIndent)
        }
    }

    fun build(root: Tree, indent: String, isLast: Boolean) : String {
        val branch = if (isLast) "└── " else "└── "
        val nextIndent = "$indent    "

        return when (root) {
            is Tree.Node -> {

                sb.apply {
                    append('\n')
                    append(indent)
                    append(branch)
                    append(nameProvider(root.value))
                }

                if (klass.isInstance(root.value)) {
                    @Suppress("UNCHECKED_CAST")
                    val children = getChildren(root.value as T, indent(isLast), nextIndent)
                    if (children.isNotEmpty()) {
                        buildChildren(children, nextIndent)
                    }
                }
                sb
            }

            is Tree.ItemList -> {
                buildChildren(listChildren(root.list), nextIndent)
                sb
            }

            is Tree.Str -> {
                sb.append(' ').append(root.str)
            }
        }.toString()
    }

    private fun getChildren(tree: T, currIndent: String, nextIndent: String): ChildrenMap {
        return childrenProvider(tree, currIndent, nextIndent).map {
            val value = it.value
            it.key to when {
                value is Collection<*> -> Tree.ItemList(value.toList())
                value is String -> Tree.Str(value)
                value != null && klass.isInstance(value) -> Tree.Node(value)
                else -> Tree.Str("$value")
            }
        }.toMap()
    }

    fun build(root: T): String {
        build(Tree.Node(root), "", false)
        return sb.toString()
    }
}

fun <T : Any> serialize(
    root: T,
    klass: KClass<T>,
    childrenProvider: (T, String, String) -> ChildrenMapRaw
): String {
    return TreeBuilder(
        klass = klass,
        childrenProvider = childrenProvider
    ).build(root)
}

fun <T : Any> serialize(
    root: T,
    indent: String = "",
    isLast: Boolean = false,
    klass: KClass<T>,
    childrenProvider: (T, String, String) -> ChildrenMapRaw
): String {
    return TreeBuilder(
        klass = klass,
        childrenProvider = childrenProvider
    ).build(
        root = Tree.Node(root),
        indent = indent,
        isLast = isLast
    )
}