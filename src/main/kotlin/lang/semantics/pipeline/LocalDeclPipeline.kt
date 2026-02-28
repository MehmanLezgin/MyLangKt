package lang.semantics.pipeline

import lang.nodes.*

class LocalDeclPipeline(
    val nameCollectionPass: NameCollectionPass,
    val declarationHeaderPass: DeclarationHeaderPass,
    val varInitPass: VarInitPass
) {
    fun execute(node: DeclStmtNode) {
        when (node) {
            is VarDeclStmtNode -> execute(node = node)
            is InterfaceDeclStmtNode -> execute(node = node)
            is ClassDeclStmtNode -> execute(node = node)
            is FuncDeclStmtNode -> execute(node = node)
            is EnumDeclStmtNode -> execute(node = node)
        }
    }

    fun execute(node: VarDeclStmtNode) {
        nameCollectionPass.resolve(node)
        declarationHeaderPass.resolve(node)
        varInitPass.resolve(node)
    }

    fun execute(node: InterfaceDeclStmtNode) {
        nameCollectionPass.resolve(node)
        declarationHeaderPass.resolve(node)
    }

    fun execute(node: ClassDeclStmtNode) {
        nameCollectionPass.resolve(node)
        declarationHeaderPass.resolve(node)
    }

    fun execute(node: FuncDeclStmtNode) {
        nameCollectionPass.resolve(node)
        declarationHeaderPass.resolve(node)
    }

    fun execute(node: EnumDeclStmtNode) {
        nameCollectionPass.resolve(node)
        declarationHeaderPass.resolve(node)
    }
}