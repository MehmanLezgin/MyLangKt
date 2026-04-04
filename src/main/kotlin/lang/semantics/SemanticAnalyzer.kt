package lang.semantics

import lang.compiler.SourceUnit
import lang.infrastructure.SourceRange
import lang.mappers.ScopeErrorMapper
import lang.messages.CompileStage
import lang.messages.MsgHandler
import lang.nodes.BaseDeclStmtNode
import lang.nodes.BaseImportStmtNode
import lang.nodes.BlockNode
import lang.nodes.ExprNode
import lang.core.PrimitivesScope
import lang.semantics.pipeline.*
import lang.semantics.resolvers.*
import lang.semantics.scopes.FileScope
import lang.semantics.scopes.Scope
import lang.semantics.scopes.ScopeError

class SemanticAnalyzer(
    override val msgHandler: MsgHandler,
) : ISemanticAnalyzer {
    override var scope: Scope = Scope(parent = PrimitivesScope)

    private var currentSourceUnit: SourceUnit? = null

    override val declResolver = DeclarationResolver(analyzer = this)
    override val constResolver = ConstResolver(analyzer = this)
    override val typeResolver = TypeResolver(analyzer = this)
    override val modResolver = ModifierResolver(analyzer = this)
    override val convertResolver = ConvertResolver(analyzer = this)
    override val overloadResolver = OverloadResolver(analyzer = this, convertResolver = convertResolver)

    override val semanticContext = SemanticContext()

    override val pipeline: AnalysisPipeline
    override val localDeclPipeline: LocalDeclPipeline
    override val typeLayoutProvider = TypeLayoutProvider(
        msgHandler = msgHandler
    )

    init {
        val moduleRegPass = ModuleRegPass(analyzer = this)
        val nameCollectionPass = NameCollectionPass(analyzer = this)
        val bindImportPass = BindImportPass(analyzer = this, moduleRegPass = moduleRegPass)
        val bindAliasPass = BindAliasPass(analyzer = this, bindImportPass = bindImportPass)
        val declarationHeaderPass = DeclarationHeaderPass(analyzer = this)
        val varInitPass = VarInitPass(analyzer = this)

        // names -> alias -> import -> decl -> var init

        pipeline = AnalysisPipeline(
            analyzer = this,
            moduleRegPass = moduleRegPass,
            passes = listOf(
                nameCollectionPass,
                bindAliasPass,
                bindImportPass,
                declarationHeaderPass,
                varInitPass,
            )
        )

        localDeclPipeline = LocalDeclPipeline(
            nameCollectionPass = nameCollectionPass,
            bindAliasPass = bindAliasPass,
            bindImportPass = bindImportPass,
            declarationHeaderPass = declarationHeaderPass,
            varInitPass = varInitPass,
        )
    }

    override fun scopeError(error: ScopeError, range: SourceRange?) {
        semanticError(msg = ScopeErrorMapper.toSecond(error), range = range)
    }

    override fun resolve(sourceUnit: SourceUnit) {
        withSourceUnit(targetSourceUnit = sourceUnit) {
            resolve(node = ast)
        }
    }

    override fun resolve(node: BlockNode) {
        node.nodes.forEach { childNode -> resolve(childNode) }
        typeResolver.resolve(target = node)
    }

    private fun resolve(node: BaseImportStmtNode) {
        localDeclPipeline.execute(node)
    }

    fun resolve(node: ExprNode) {
        when (node) {
            is BaseDeclStmtNode -> declResolver.resolve(target = node)
            is BlockNode -> resolve(node = node)
            is BaseImportStmtNode -> resolve(node = node)
            else -> typeResolver.resolve(target = node)
        }
    }

    override fun executePipeline(sources: List<SourceUnit>) {
        pipeline.execute(sources = sources)
    }

    override fun <T> withScope(
        targetScope: Scope,
        block: () -> T
    ): T {
        val prev = scope
        scope = targetScope
        try {
            return block()
        } finally {
            scope = prev
        }
    }

    fun withSourceUnit(
        targetSourceUnit: SourceUnit,
        block: SourceUnit.() -> Unit
    ) {
        val targetScope = targetSourceUnit.scope
            ?: FileScope(parent = scope, scopeName = "")

        withScope(targetScope) {
            val prev = currentSourceUnit
            currentSourceUnit = targetSourceUnit
            try {
                targetSourceUnit.apply(block)
            } finally {
                currentSourceUnit = prev
            }
        }
    }


    override fun withScopeResolveBody(targetScope: Scope, body: BlockNode?) {
        if (body == null) return
        withScope(targetScope = targetScope) {
            resolve(node = body)
        }
    }

    private fun semanticError(msg: String, range: SourceRange?) {
        msgHandler.semanticError(msg, range)
    }

    override fun warning(msg: String, range: SourceRange) =
        msgHandler.warn(msg = msg, range = range, stage = CompileStage.SEMANTIC_ANALYSIS)
}