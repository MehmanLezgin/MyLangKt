package lang.semantics.pipeline

import lang.nodes.*

class LocalDeclPipeline(
    val nameCollectionPass: NameCollectionPass,
    val bindAliasPass: BindAliasPass,
    val bindImportPass: BindImportPass,
    val declarationHeaderPass: DeclarationHeaderPass,
    val varInitPass: VarInitPass
) {
    fun execute(node: StmtNode) {
        when (node) {
            is VarDeclStmtNode -> execute(node = node)
            is InterfaceDeclStmtNode -> execute(node = node)
            is ClassDeclStmtNode -> execute(node = node)
            is FuncDeclStmtNode -> execute(node = node)
            is EnumDeclStmtNode -> execute(node = node)
            is BaseImportStmtNode -> execute(node = node)
            is UsingDirectiveNode -> execute(node = node)
            is TemplateStmtNode -> execute(node = node)
            else -> Unit
        }
    }

    fun execute(node: TemplateStmtNode) {
        nameCollectionPass.resolve(node)
    }

    fun execute(node: UsingDirectiveNode) {
        bindAliasPass.resolve(node)
    }

    fun execute(node: BaseImportStmtNode) {
        bindImportPass.resolve(node)
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