package lang.parser

import lang.core.KeywordType
import lang.core.LangSpec.moduleNameSeparator
import lang.core.SourceRange
import lang.core.operators.OperatorType
import lang.mappers.ModifierMapper
import lang.messages.Msg
import lang.messages.Terms
import lang.messages.Terms.plural
import lang.nodes.*
import lang.parser.ParserUtils.flattenCommaNode
import lang.parser.ParserUtils.isBinOperator
import lang.parser.ParserUtils.isKeyword
import lang.parser.ParserUtils.isNotOperator
import lang.parser.ParserUtils.range
import lang.parser.ParserUtils.toBlockNode
import lang.parser.ParserUtils.toDatatype
import lang.parser.ParserUtils.toIdentifierNode
import lang.parser.ParserUtils.wrapToBlock
import lang.tokens.ITokenStream
import lang.tokens.Token

class StmtParser(
    private val ts: ITokenStream,
    private val parser: IParser,
) : IStmtParser {

    private val modifierMapper = ModifierMapper()

    override fun parse(isSingleLine: Boolean): ExprNode {
        ts.skipTokens(Token.Semicolon::class)
        val t = ts.peek()

        if (t !is Token.Keyword) {

            if (ts.match(Token.EOF::class))
                return VoidNode

            if (ts.match(Token.RBrace::class)) {
                syntaxError(Msg.EXPECTED_TOP_LEVEL_DECL, t.range)
                ts.next()
                return VoidNode
            }

            if (ts.match(Token.RParen::class, Token.RBracket::class)) {
                syntaxError(Msg.UNEXPECTED_TOKEN, t.range)
                ts.next()
                return VoidNode
            }

            val expr = parser.parseExpr()

            if (!isSingleLine && !ts.match(Token.RBrace::class, Token.LBrace::class))
                ts.expectSemicolonOrLinebreak(Msg.EXPECTED_SEMICOLON)

            return expr
        }

        fun errorStmt(msg: String): () -> ExprNode {
            syntaxError(msg, t.range)
            ts.next()
            return ::voidExprFunc
        }

        val parserFunc = when (t.type) {
            KeywordType.VAR,
            KeywordType.LET -> ::parseVarDeclStmt

            KeywordType.FUNC -> ::parseFuncDeclStmt

            KeywordType.DO -> ::parseDoWhileStmt
            KeywordType.WHILE -> ::parseWhileStmt
            KeywordType.MATCH -> ::parseMatchStmt
            KeywordType.IF -> ::parseIfElseStmt
            KeywordType.ELSE -> ::parseElseEntryStmt
            KeywordType.ELIF -> errorStmt(Msg.EXPECTED_IF)

            KeywordType.CONST, KeywordType.STATIC,
            KeywordType.OPEN, KeywordType.ABSTRACT,
            KeywordType.OVERRIDE, KeywordType.INFIX,
            KeywordType.PRIVATE, KeywordType.PUBLIC,
            KeywordType.PROTECTED ->
                ::parseDeclarationWithModifiers

            KeywordType.CONTINUE -> ::parseContinueStmt
            KeywordType.BREAK -> ::parseBreakStmt

            KeywordType.CATCH,
            KeywordType.FINALLY -> errorStmt(Msg.EXPECTED_TRY)

            KeywordType.RETURN -> ::parseReturnStmt
            KeywordType.CLASS -> ::parseClassStmt
            KeywordType.INTERFACE -> ::parseInterfaceStmt
            KeywordType.ENUM -> ::parseEnumStmt
            KeywordType.CONSTRUCTOR -> ::parseConstructorStmt
            KeywordType.DESTRUCTOR -> ::parseDestructorStmt

            KeywordType.FOR -> ::parseForLoopStmt
            KeywordType.TRY -> ::parseTryCatchStmt
            KeywordType.MODULE -> ::parseModuleStmt
            KeywordType.USING -> ::parseUsingStmt
            KeywordType.OPERATOR -> errorStmt(Msg.EXPECTED_FUNC_DECL)
            KeywordType.IMPORT -> ::parseImportModuleStmt
            KeywordType.FROM -> ::parseFromImportStmt
        }

        return parserFunc() ?: VoidNode
    }

    private fun parseUsingStmt(): ExprNode? {
        return ts.captureRange {
            ts.next()

            var expr = parser.parseExpr(ctx = ParsingContext.Condition)

            if (ts.match(Token.LBrace::class)) {
                val body = parseBlock()
                return@captureRange UsingStmtNode(
                    scopedExpr = expr,
                    body = body,
                    range = resultRange
                )
            }

            expr = parser.analiseAsDatatype(
                expr = expr,
                allowAsExpression = true
            ) ?: return@captureRange null


            if (expr isBinOperator BinOpType.COMMA) {
                val allUsingNodes = expr.flattenCommaNode().mapNotNull { usingNode ->
                    buildUsingDirective(
                        expr = usingNode,
                        resultRange = usingNode.range
                    )
                }

                val block = allUsingNodes.toBlockNode(resultRange)
                return@captureRange block

            }

            return@captureRange buildUsingDirective(
                expr = expr,
                resultRange = resultRange
            )
        }
    }


    private fun buildUsingDirective(
        expr: ExprNode,
        resultRange: SourceRange
    ): UsingDirectiveNode? {
        return when (expr) {
            is BinOpNode -> {
                val left = expr.left
                if (expr.operator != BinOpType.ASSIGN || left !is IdentifierNode) {
                    syntaxError(Msg.EXPECTED_IDENTIFIER, expr.range)
                    return null
                }

                UsingDirectiveNode(
                    name = left,
                    value = expr.right,
                    range = resultRange
                )
            }

            is QualifiedDatatypeNode,
            is IdentifierNode -> UsingDirectiveNode(
                name = null,
                value = expr,
                range = resultRange
            )

            else -> {
                syntaxError(Msg.EXPECTED_IDENTIFIER, expr.range)
                null
            }
        }
    }


    override fun parseBlock(): BlockNode {
        if (ts.matchOperator(OperatorType.COLON))
            ts.next()

        val isMultilineBody = ts.match(Token.LBrace::class)

        val list = mutableListOf<ExprNode>()

        return if (isMultilineBody) {
            ts.captureRange {
                ts.next()

                while (!ts.match(Token.RBrace::class, Token.EOF::class)) {
                    val expr = parse()
                    if (expr != VoidNode)
                        list.add(expr)
                    ts.skipTokens(Token.Semicolon::class)
                }

                if (ts.expect(Token.RBrace::class, Msg.EXPECTED_RBRACE))
                    ts.next()

                BlockNode(nodes = list, range = resultRange)
            }
        } else {
            val expr = parse(isSingleLine = true).wrapToBlock()
            ts.skipTokens(Token.Semicolon::class)
            expr
        }
    }

    private fun parseTryCatchStmt(): TryCatchStmtNode {
        return ts.captureRange {
            ts.next()
            val tryBody = parseBlock()

            var catchParam: ExprNode? = null
            var catchBody: BlockNode? = null
            var finallyBody: BlockNode? = null

            if (ts.peek() isKeyword KeywordType.CATCH) {
                ts.next()
                val pair = parseConditionAndBody()
                catchParam = pair.first
                catchBody = pair.second
            }

            if (ts.peek() isKeyword KeywordType.FINALLY) {
                ts.next()
                finallyBody = parseBlock()
            }

            if (catchBody == null && finallyBody == null) {
                syntaxError(Msg.EXPECTED_CATCH, ts.range)
            }

            TryCatchStmtNode(
                tryBody = tryBody,
                catchParam = catchParam,
                catchBody = catchBody,
                finallyBody = finallyBody,
                range = resultRange
            )
        }
    }

    private fun parseImportModuleStmt(): BaseImportStmtNode? {
        return ts.captureRange {
            ts.next()

            val clause = parser.parseNameClause()

            // example: import sym, ... from mod1::mod2
            if (ts.peek() isKeyword KeywordType.FROM) {
                ts.next()

                val sourceName = parser.parseNameSpecifier()
                    ?: return@captureRange null

                return@captureRange ImportFromStmtNode(
                    sourceName = sourceName,
                    items = clause,
                    range = resultRange
                )
            }

            // example: import mod1, ...
            ImportModulesStmtNode(
                items = clause,
                range = resultRange
            )
        }
    }


    private fun parseFromImportStmt(): ImportFromStmtNode? {
        return ts.captureRange {
            ts.next()

            val sourceName = parser.parseNameSpecifier()
                ?: return@captureRange null

            if (!ts.expectKeyword(KeywordType.IMPORT, Msg.EXPECTED_IMPORT))
                return@captureRange null

            ts.next()

            val clause = parser.parseNameClause()

            ImportFromStmtNode(
                sourceName = sourceName,
                items = clause,
                range = resultRange
            )
        }
    }

    private fun parseModuleStmt(): ModuleStmtNode? {
        return ts.captureRange {
            ts.next()

            val list = parser.parseIdsWithSeparatorOper(separator = moduleNameSeparator)
                ?.reversed()

            if (list.isNullOrEmpty()) return@captureRange null

            if (ts.match(Token.LBrace::class)) {
                val body = parseBlock()

                val module = buildModuleHierarchy(
                    list = list,
                    body = body,
                    range = resultRange
                )

                return@captureRange module
            } else {
                if (parser.moduleName != null) {
                    syntaxError(Msg.SRC_CAN_CONTAIN_ONE_FILE_MODULE_DECL, resultRange)
                } else
                    parser.moduleName = QualifiedName(list)

                null
            }
        }
    }

    override fun buildModuleHierarchy(
        list: List<IdentifierNode>,
        body: BlockNode,
        range: SourceRange
    ): ModuleStmtNode? {
        var module: ModuleStmtNode? = null

        list.forEachIndexed { i, id ->
            val moduleBody = if (i == 0) body else module!!.wrapToBlock()

            module = ModuleStmtNode(
                name = id,
                body = moduleBody,
                range = range
            )
        }

        return module
    }

    private fun parseConstructorStmt(): ConstructorDeclStmtNode {
        return ts.captureRange {
            ts.next()

            var params: List<VarDeclStmtNode>? = null

            if (ts.match(Token.LParen::class))
                analiseParams(parser.parseArgsList()) { params = it }

            val body = parseBlock()

            ConstructorDeclStmtNode(
                modifiers = null,
                params = params ?: emptyList(),
                body = body,
                range = resultRange
            )
        }
    }

    private fun parseDestructorStmt(): DestructorDeclStmtNode {
        return ts.captureRange {
            ts.next()

            if (ts.match(Token.LParen::class)) {
                val args = parser.parseArgsList() // skipping

                if (args.isNotEmpty())
                    syntaxError(Msg.CONSTRUCTORS_CANNOT_HAVE_PARAMS, startRange)
            }

            val body = parseBlock()

            DestructorDeclStmtNode(
                modifiers = null,
                body = body,
                range = resultRange
            )
        }
    }

    override fun analiseDatatypeList(exprList: List<ExprNode>?): List<BaseDatatypeNode>? {
        if (exprList.isNullOrEmpty())
            return null

        val datatypes: MutableList<BaseDatatypeNode> = mutableListOf()

        exprList.forEach { expr ->
            val typeName: BaseDatatypeNode? = when (expr) {
                is IdentifierNode -> expr.toDatatype()

                is BaseDatatypeNode -> expr

                else -> {
                    syntaxError(Msg.EXPECTED_TYPE_PARAM_NAME, expr.range)
                    null
                }
            }

            if (typeName != null)
                datatypes.add(typeName)
        }

        return datatypes
    }

    private fun buildVarDeclHeader(
        header: ExprNode,
        range: SourceRange = header.range,
        isMutable: Boolean = true
    ): VarDeclStmtNode? {
        var name: IdentifierNode? = null
        var dataType: BaseDatatypeNode? = null
        var initializer: ExprNode? = null

        analiseHeader(
            header = header,
            errorMsg = Msg.EXPECTED_VAR_DECL,
            handleName = {
                if (it !is IdentifierNode) {
                    syntaxError(Msg.EXPECTED_IDENTIFIER, it.range)
                    return@analiseHeader
                }

                name = it
            },
            handleTypeNames = {
                if (it == null) return@analiseHeader
                syntaxError(
                    Msg.X_CANNOT_HAVE_Y
                        .format(Terms.VARIABLE, Terms.TYPE_NAME.plural(it.params.size)),
                    it.range
                )
            },
            handleParams = {
                if (it == null) return@analiseHeader
                syntaxError(
                    Msg.X_CANNOT_HAVE_Y
                        .format(Terms.VARIABLE, Terms.PARAM.plural(it.size)),
                    it.range(header.range)
                )
            },
            handleSuperType = { dataType = it },
            handleInitializer = {
                initializer = if (it is VoidNode) null else it
            }
        )

        return VarDeclStmtNode(
            modifiers = null,
            isMutable = isMutable,
            name = name ?: return null,
            dataType = dataType ?: AutoDatatypeNode(range),
            initializer = initializer,
            range = range
        )
    }

    private fun buildFuncDeclStmt(
        header: ExprNode,
        typeNames: TypeNameListNode?,
        body: BlockNode?,
        range: SourceRange
    ): FuncDeclStmtNode? {
        var name: ExprNode? = null
        var params: List<VarDeclStmtNode>? = null
        var returnType: BaseDatatypeNode? = null
        var initializerBody: ExprNode? = null

        analiseHeader(
            header = header,
            errorMsg = Msg.EXPECTED_FUNC_DECL,
            handleName = {
                name = it
            },
            handleTypeNames = {
                if (it != null)
                    syntaxError(Msg.TYPE_NAMES_MUST_BE_PLACES_BEFORE_FUNC_NAME, it.range)
            },
            handleParams = { params = it },
            handleSuperType = {
                returnType =
                    if (it is AutoDatatypeNode) VoidDatatypeNode(it.range)
                    else it
            },
            handleInitializer = { initializerBody = it }
        )

        val finalReturnType =
            returnType ?: if (body == null) AutoDatatypeNode(range) else VoidDatatypeNode(range)

        return FuncDeclStmtNode(
            modifiers = null,
            name = name ?: return null,
            params = params ?: emptyList(),
            typeNames = typeNames,
            returnType = finalReturnType,
            body = initializerBody?.wrapToBlock() ?: body,
            range = range
        )
    }

    private fun buildClassStmt(
        header: ExprNode,
        body: BlockNode?,
        range: SourceRange
    ): ClassDeclStmtNode? {
        var name: IdentifierNode? = null
        var typeNames: TypeNameListNode? = null
        var superClass: BaseDatatypeNode? = null
        var primaryConstrParams: List<VarDeclStmtNode>? = null
        var initializerBody: ExprNode? = null

        analiseHeader(
            header = header,
            errorMsg = Msg.EXPECTED_CLASS_DECL,
            handleName = {
                if (it !is IdentifierNode) {
                    syntaxError(Msg.EXPECTED_IDENTIFIER, it.range)
                    return@analiseHeader
                }

                name = it
            },
            handleTypeNames = { typeNames = it },
            handleParams = { primaryConstrParams = it },
            handleSuperType = { superClass = it },
            handleInitializer = { initializerBody = it }
        )

        return ClassDeclStmtNode(
            modifiers = null,
            name = name ?: return null,
            primaryConstrParams = primaryConstrParams,
            typeNames = typeNames,
            superClass = superClass ?: VoidDatatypeNode(range),
            body = initializerBody?.wrapToBlock() ?: body,
            range = range
        )
    }

    private fun buildInterfaceStmt(
        header: ExprNode,
        body: BlockNode?,
        range: SourceRange
    ): InterfaceDeclStmtNode? {
        var name: IdentifierNode? = null
        var typeNames: TypeNameListNode? = null
        var superInterface: BaseDatatypeNode? = null
        var initializerBody: ExprNode? = null

        analiseHeader(
            header = header,
            errorMsg = Msg.EXPECTED_INTERFACE_DECL,
            handleName = {
                if (it !is IdentifierNode) {
                    syntaxError(Msg.EXPECTED_IDENTIFIER, it.range)
                    return@analiseHeader
                }

                name = it
            },
            handleTypeNames = { typeNames = it },
            handleParams = {
                syntaxError(Msg.INTERFACES_CANNOT_HAVE_CONSTRUCTORS, header.range)
            },
            handleSuperType = { superInterface = it },
            handleInitializer = { initializerBody = it }
        )

        return InterfaceDeclStmtNode(
            modifiers = null,
            name = name ?: return null,
            typeNames = typeNames,
            superInterface = superInterface ?: VoidDatatypeNode(range),
            body = initializerBody?.wrapToBlock() ?: body,
            range = range
        )
    }


    private fun analiseHeader(
        header: ExprNode,
        errorMsg: String,
        handleName: (ExprNode) -> Unit,
        handleTypeNames: (TypeNameListNode?) -> Unit,
        handleParams: (List<VarDeclStmtNode>?) -> Unit,
        handleSuperType: (BaseDatatypeNode?) -> Unit,
        handleInitializer: (ExprNode?) -> Unit
    ) {
        fun analiseSuperType(expr: ExprNode) {
            handleSuperType(parser.analiseAsDatatype(expr) as BaseDatatypeNode?)
        }

        fun handleNameAndParams(expr: ExprNode) {
            analiseNameAndParams(
                expr = expr,
                msg = errorMsg,
                handleName = handleName,
                handleTypeNames = handleTypeNames,
                handleParams = handleParams
            )
        }

        when (header) {
            is IdentifierNode -> {
                // only name: func fn {}
                analiseNameNode(header, errorMsg, handleName, handleTypeNames)
            }

            is DatatypeNode -> {
                handleNameAndParams(header)
            }

            is ScopedDatatypeNode -> {
                handleName(header)
            }

            is DotAccessNode -> {
                handleName(header)
            }

            is FuncCallNode -> {
                // with params, but without return type: func fn(...) {}
                handleNameAndParams(header)
            }

            is BinOpNode -> {
                // with name and datatype, with params or not:
                // func fn : int    or    func fn(a: int) : int

                when (header.operator) {
                    BinOpType.ASSIGN -> {
                        analiseHeader(
                            header = header.left,
                            errorMsg = errorMsg,
                            handleName = handleName,
                            handleTypeNames = handleTypeNames,
                            handleParams = handleParams,
                            handleSuperType = handleSuperType,
                            handleInitializer = {}

                        )

                        handleInitializer(header.right)
                    }

                    BinOpType.COLON -> {
                        handleNameAndParams(header.left)
                        analiseSuperType(header.right)
                    }

                    else -> {
                        syntaxError(errorMsg, header.range)
                        return
                    }
                }
            }

            else -> {
                syntaxError(errorMsg, header.range)
                return
            }
        }
    }

    private fun parseBodyForDeclStmt(): BlockNode? {
        return when {
            ts.match(Token.LBrace::class) -> parseBlock()

            else -> {
                ts.expectSemicolonOrLinebreak(Msg.EXPECTED_SEMICOLON)
                null
            }
        }
    }

    private fun parseFuncDeclStmt(): FuncDeclStmtNode? {
        return ts.captureRange {
            ts.next()

            val typeNames = if (ts.matchOperator(OperatorType.LESS))
                parser.parseTypenameList() else null

            val header = parser.parseExpr(ctx = ParsingContext.FuncHeader)

            val body = if (ts.match(Token.LBrace::class))
                parseBodyForDeclStmt()
            else null

            buildFuncDeclStmt(header, typeNames, body, resultRange)
        }
    }

    private fun parseClassStmt(): ClassDeclStmtNode? {
        return ts.captureRange {
            ts.next()

            val header = parser.parseExpr(ctx = ParsingContext.Header)

            val body = parseBodyForDeclStmt()

            buildClassStmt(
                header = header,
                body = body,
                range = resultRange
            )
        }
    }

    private fun parseInterfaceStmt(): InterfaceDeclStmtNode? {
        return ts.captureRange {
            ts.next()
            val header = parser.parseExpr(ctx = ParsingContext.Header)
            val body = parseBodyForDeclStmt()

            buildInterfaceStmt(
                header = header,
                body = body,
                range = resultRange
            )
        }
    }

    private fun parseVarDeclStmt(): VarDeclStmtNode? {
        val isMutable = ts.peek() isKeyword KeywordType.VAR
        return ts.captureRange {
            ts.next()
            val expr = parser.parseExpr(ctx = ParsingContext.Header)

            buildVarDeclHeader(
                header = expr,
                range = resultRange,
                isMutable = isMutable
            )
        }
    }

    private fun parseEnumStmt(): EnumDeclStmtNode? {
        return ts.captureRange {
            ts.next()

            val nameToken = ts.peek()

            val enumName = if (nameToken is Token.Identifier) {
                ts.next()
                nameToken.toIdentifierNode()
            } else {
                syntaxError(Msg.NAME_EXPECTED, nameToken.range)
                return@captureRange null
            }

            if (!ts.match(Token.LBrace::class)) {
                syntaxError(Msg.UNEXPECTED_TOKEN, ts.range)
                ts.skipUntil(
                    Token.LBrace::class,
                    Token.RBrace::class,
                    Token.Keyword::class,
                    Token.Identifier::class
                )
                return@captureRange EnumDeclStmtNode(
                    modifiers = null,
                    name = enumName,
                    body = BlockNode.empty(resultRange),
                    range = resultRange
                )
            }


            if (!ts.expect(Token.LBrace::class, Msg.EXPECTED_LBRACE))
                return@captureRange EnumDeclStmtNode(
                    modifiers = null,
                    name = enumName,
                    body = BlockNode.empty(resultRange),
                    range = resultRange
                )

            val items = parseBlock()

            EnumDeclStmtNode(
                modifiers = null,
                name = enumName,
                body = items,
                range = resultRange
            )
        }
    }

    private fun analiseNameNode(
        expr: ExprNode,
        msg: String,
        handleName: (ExprNode) -> Unit,
        handleTypeNames: (TypeNameListNode?) -> Unit
    ) {
        when (expr) {
            is IdentifierNode -> {
                handleName(expr)
            }

            is ScopedDatatypeNode -> {
                handleName(expr)
                handleTypeNames(expr.member.typeNames)
            }

            is DotAccessNode -> {
                handleName(expr)
            }

            else -> syntaxError(msg, expr.range)
        }
    }

    private fun analiseParams(
        exprList: List<ExprNode>,
        handleParams: (List<VarDeclStmtNode>?) -> Unit
    ) {
        handleParams(analiseParams(exprList))
    }

    override fun analiseParams(exprList: List<ExprNode>): List<VarDeclStmtNode> {
        val params: MutableList<VarDeclStmtNode> = mutableListOf()

        exprList.forEach { expr ->
            val param = buildVarDeclHeader(expr, isMutable = false)
            if (param != null) params.add(param)
        }

        return params
    }

    private fun analiseNameAndParams(
        expr: ExprNode,
        msg: String,
        handleName: (ExprNode) -> Unit,
        handleTypeNames: (TypeNameListNode?) -> Unit,
        handleParams: (List<VarDeclStmtNode>?) -> Unit
    ) {
        when (expr) {
            is IdentifierNode -> {
                handleName(expr)
            }

            is FuncCallNode -> {
                analiseNameNode(expr.receiver, msg, handleName, handleTypeNames)
                handleTypeNames(expr.typeNames)
                analiseParams(expr.args, handleParams)
            }

            is DatatypeNode -> {
                handleName(expr.identifier)
                handleTypeNames(expr.typeNames)
            }

            else -> {
                syntaxError(Msg.EXPECTED_FUNC_DECL, expr.range)
            }
        }
    }

    private fun parseContinueStmt() = ContinueStmtNode(range = ts.next().range)

    private fun parseBreakStmt() = BreakStmtNode(range = ts.next().range)

    private fun parseDeclarationWithModifiers(): DeclStmtNode<*>? {
        val modifiers = parseModifiers()
        val range = ts.range

        val stmt = parse()

        val stmtWithMod = when {
            stmt is DeclStmtNode<*> -> {
                stmt.modifiers = modifiers
                stmt
            }

            else -> {
                syntaxError(Msg.EXPECTED_A_DECLARATION, range)
                null
            }
        }

        return stmtWithMod
    }

    private fun parseModifiers(): ModifierSetNode {
        return ts.captureRange {
            val modifiers = mutableSetOf<ModifierNode>()

            while (true) {
                val t = ts.peek()

                if (t !is Token.Keyword)
                    break

                val modifier = modifierMapper.toSecond(t) ?: break

                if (modifiers.any { it::class == modifier::class }) {
                    syntaxError(
                        Msg.RepeatedModifier.format(modifier.keyword.value),
                        t.range
                    )
                    ts.next()
                    continue
                }

                modifiers.add(modifier)
                ts.next()
            }

            ModifierSetNode(nodes = modifiers, range = resultRange)
        }
    }

    private fun voidExprFunc() = VoidNode

    private fun parseConditionAndBody(): Pair<ExprNode, BlockNode> {
        val isConditionWithParens = ts.match(Token.LParen::class)

        val condition = parser.parseExpr(ctx = ParsingContext.Condition)

        if (!isConditionWithParens) {
            ts.expect(
                Token.LBrace::class,
                Token.Semicolon::class,
                msg = Msg.EXPECTED_LBRACE_AFTER_CONDITION
            )
        }

        val body = parseBlock()
        return condition to body
    }

    override fun parseIfElseStmt(): IfElseStmtNode {
        return ts.captureRange {
            ts.next()

            val (condition, body) = parseConditionAndBody()

            val t = ts.peek()

            val elseBody =
                when {
                    t isKeyword KeywordType.ELSE -> {
                        ts.next()
                        parseBlock()
                    }

                    t isKeyword KeywordType.ELIF -> {
                        parseIfElseStmt().wrapToBlock()
                    }

                    else -> null
                }

            IfElseStmtNode(
                condition = condition,
                body = body,
                elseBody = elseBody,
                range = resultRange
            )
        }
    }

    private fun parseWhileStmt(): WhileStmtNode {
        return ts.captureRange {
            ts.next()

            val (condition, body) = parseConditionAndBody()

            val elseBody = if (ts.peek() isKeyword KeywordType.ELSE) {
                ts.next()
                parseBlock()
            } else null

            WhileStmtNode(
                condition = condition,
                body = body,
                elseBody = elseBody,
                range = resultRange
            )
        }
    }

    private fun parseDoWhileStmt(): DoWhileStmtNode {
        return ts.captureRange {
            ts.next()

            val body = parseBlock()

            ts.expectKeyword(
                KeywordType.WHILE,
                Msg.EXPECTED_WHILE_AND_POST_CONDITION
            )
            ts.next()

            val condition = parser.parseExpr(ctx = ParsingContext.Condition)

            DoWhileStmtNode(
                condition = condition,
                body = body,
                range = resultRange
            )
        }
    }

    private fun parseForLoopStmt(): ForLoopStmtNode {
        return ts.captureRange {
            ts.next()

            val (condition, body) = parseConditionAndBody()

            ForLoopStmtNode(
                condition = condition,
                body = body,
                range = resultRange
            )
        }
    }


    private fun parseElseEntryStmt(): ElseEntryNode? {
        return ts.captureRange {
            ts.next()
            val t = ts.peek()

            if (t isNotOperator OperatorType.ARROW) {
                syntaxError(Msg.EXPECTED_ARROW_OPERATOR, t.range)
                return@captureRange null
            }

            ts.next()

            ElseEntryNode(
                expr = parse(),
                range = resultRange
            )
        }
    }

    private fun parseMatchStmt(): MatchStmtNode {
        return ts.captureRange {
            ts.next()

            var target: ExprNode?
            var body: BlockNode

            if (ts.match(Token.LBrace::class)) {
                target = null
                body = parseBlock()
            } else {
                val pair = parseConditionAndBody()
                target = pair.first
                body = pair.second
            }

            MatchStmtNode(
                target = target,
                body = body,
                range = resultRange
            )
        }
    }


    private fun parseReturnStmt(): ReturnStmtNode {
        return ts.captureRange {
            ts.next()

            val stmt = if (ts.matchSemicolonOrLinebreak())
                VoidNode
            else parse()

            ReturnStmtNode(
                expr = stmt,
                range = resultRange
            )
        }
    }

    private fun syntaxError(msg: String, range: SourceRange) =
        parser.syntaxError(msg = msg, range = range)

    private fun warning(msg: String, range: SourceRange) =
        parser.warning(msg = msg, range = range)
}