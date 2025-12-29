package lang.semantics

import lang.messages.ErrorHandler
import lang.nodes.DatatypeNode
import lang.nodes.IdentifierNode
import lang.semantics.scopes.InterfaceScope
import lang.semantics.scopes.Scope
import lang.semantics.symbols.BuiltInTypeSymbol
import lang.semantics.symbols.InterfaceSymbol
import lang.semantics.symbols.TypedefSymbol
import lang.tokens.Pos

object BuiltInTypes {
    fun initBuiltInTypes(
        scope: Scope,
        errorHandler: ErrorHandler
    ) {
        arrayOf(
            "int8"      to arrayOf("byte", "char"),
            "int16"     to arrayOf("short"),
            "int32"     to arrayOf("int"),
            "int64"     to arrayOf("long"),
            "uint8"     to arrayOf("ubyte", "uchar", "bool"),
            "uint16"    to arrayOf("ushort"),
            "uint32"    to arrayOf("uint"),
            "uint64"    to arrayOf("ulong"),
            "float"     to arrayOf("float32"),
            "double"    to arrayOf("float64")
        ).forEach { (name, aliases) ->
            val pos = Pos()

            scope.define(
                sym = BuiltInTypeSymbol(
                    name = name,
                    scope = Scope(
                        parent = scope,
                        errorHandler = errorHandler
                    )
                ),
                pos = pos
            )

            val typename = DatatypeNode(
                identifier = IdentifierNode(value = name, pos = pos),
                pos = pos
            )

            aliases.forEach { aliasName ->
                scope.define(
                    sym = TypedefSymbol(
                        name = aliasName,
                        typename = typename
                    ),
                    pos = pos
                )
            }
        }

        /*arrayOf(
            "float", "double", "long", "short", "char", "byte",
            "int", "int8", "int16", "int32", "int64",
            "uint", "uint8", "uint16", "uint32", "uint64", "void"
        )*/
    }

}