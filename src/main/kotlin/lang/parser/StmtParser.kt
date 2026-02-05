package lang.parser

import lang.tokens.KeywordType
import lang.messages.Msg
import lang.tokens.OperatorType
import lang.tokens.ITokenStream
import lang.tokens.Pos
import lang.tokens.Token
import lang.mappers.ModifierMapper
import lang.nodes.AutoDatatypeNode
import lang.nodes.BaseDatatypeNode
import lang.nodes.BinOpNode
import lang.nodes.BinOpType
import lang.nodes.BlockNode
import lang.nodes.BreakStmtNode
import lang.nodes.ClassDeclStmtNode
import lang.nodes.ConstructorDeclStmtNode
import lang.nodes.ContinueStmtNode
import lang.nodes.DatatypeNode
import lang.nodes.DeclStmtNode
import lang.nodes.DestructorDeclStmtNode
import lang.nodes.DoWhileStmtNode
import lang.nodes.ElseEntryNode
import lang.nodes.EnumDeclStmtNode
import lang.nodes.ExprNode
import lang.nodes.ForLoopStmtNode
import lang.nodes.FuncCallNode
import lang.nodes.FuncDeclStmtNode
import lang.nodes.IdentifierNode
import lang.nodes.IfElseStmtNode
import lang.nodes.ImportKind
import lang.nodes.ImportStmtNode
import lang.nodes.InterfaceDeclStmtNode
import lang.nodes.MatchStmtNode
import lang.nodes.ModifierNode
import lang.nodes.ModifierSetNode
import lang.nodes.NamespaceStmtNode
import lang.nodes.ReturnStmtNode
import lang.nodes.TryCatchStmtNode
import lang.nodes.TypeNameListNode
import lang.nodes.TypeNameNode
import lang.nodes.TypedefStmtNode
import lang.nodes.VarDeclStmtNode
import lang.nodes.VoidDatatypeNode
import lang.nodes.VoidNode
import lang.nodes.WhileStmtNode
import lang.parser.ParserUtils.isKeyword
import lang.parser.ParserUtils.isNotOperator
import lang.parser.ParserUtils.toDatatype
import lang.parser.ParserUtils.toIdentifierNode
import lang.parser.ParserUtils.tryConvertToDatatype
import lang.parser.ParserUtils.wrapToBody

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
                syntaxError(Msg.EXPECTED_TOP_LEVEL_DECL, t.pos)
                ts.next()
                return VoidNode
            }

            if (ts.match(Token.RParen::class, Token.RBracket::class)) {
                syntaxError(Msg.UNEXPECTED_TOKEN, t.pos)
                ts.next()
                return VoidNode
            }

            val expr = parser.parseExpr()

            if (!isSingleLine && !ts.match(Token.RBrace::class, Token.LBrace::class))
                ts.expectSemicolonOrLinebreak(Msg.EXPECTED_SEMICOLON)

            return expr
        }

        fun errorStmt(msg: String): () -> ExprNode {
            syntaxError(msg, t.pos)
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

            KeywordType.CONST, KeywordType.STATIC, KeywordType.OPEN, KeywordType.ABSTRACT,
            KeywordType.OVERRIDE,
            KeywordType.PRIVATE, KeywordType.PUBLIC, KeywordType.PROTECTED, KeywordType.EXPORT ->
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
            KeywordType.NAMESPACE -> ::parseNamespaceStmt
            KeywordType.TYPE -> ::parseTypedefStmt
            KeywordType.USING -> errorStmt(Msg.USING_NOT_IMPL)
            KeywordType.OPERATOR -> errorStmt(Msg.EXPECTED_FUNC_DECL)
            KeywordType.MODULE -> errorStmt(Msg.MODULE_IS_NOT_AT_START)
            KeywordType.IMPORT -> ::parseImportStmt
            KeywordType.FROM -> ::parseFromImportStmt
        }

        return parserFunc() ?: VoidNode
    }

    private fun parseTypedefStmt(): TypedefStmtNode? {
        val pos = ts.next().pos

        if (!ts.expect(Token.Identifier::class, Msg.EXPECTED_IDENTIFIER))
            return null

        val identifier = (ts.next() as Token.Identifier).toIdentifierNode()

        if (ts.peek() isNotOperator OperatorType.ASSIGN) {
            syntaxError(Msg.EXPECTED_ASSIGN, ts.pos)
            return null
        }

        ts.next()
        val datatypeNode = analiseAsDatatype(
            expr = parser.parseExpr(ctx = ParsingContext.TypeArg),
            allowAsExpression = false
        ) as? BaseDatatypeNode? ?: return null

        return TypedefStmtNode(
            name = identifier,
            dataType = datatypeNode,
            pos = pos
        )
    }

    override fun parseBlock(): BlockNode {
        if (ts.matchOperator(OperatorType.COLON))
            ts.next()

        val isMultilineBody = ts.match(Token.LBrace::class)

        val list = mutableListOf<ExprNode>()

        return if (isMultilineBody) {
            val pos = ts.pos
            ts.next()

            while (!ts.match(Token.RBrace::class, Token.EOF::class)) {
                list.add(parse())
                ts.skipTokens(Token.Semicolon::class)
            }

            if (ts.expect(Token.RBrace::class, Msg.EXPECTED_RBRACE))
                ts.next()

            BlockNode(nodes = list, pos = pos)
        } else {
            val expr = parse(isSingleLine = true).wrapToBody()
            ts.skipTokens(Token.Semicolon::class)
            expr
        }
    }

    private fun parseTryCatchStmt(): TryCatchStmtNode {
        val pos = ts.next().pos
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
            syntaxError(Msg.EXPECTED_CATCH, ts.pos)
        }

        return TryCatchStmtNode(
            tryBody = tryBody,
            catchParam = catchParam,
            catchBody = catchBody,
            finallyBody = finallyBody,
            pos = pos
        )
    }

    private fun parseImportStmt(): ImportStmtNode? {
        val pos = ts.next().pos

        val moduleName = parser.parseModuleName(withModuleKeyword = false)
            ?: return null

        return ImportStmtNode(
            moduleName = moduleName,
            kind = ImportKind.Module,
            pos = pos
        )
    }

    private fun parseFromImportStmt(): ImportStmtNode? {
        val pos = ts.next().pos

        val moduleName = parser.parseModuleName(withModuleKeyword = false)
            ?: return null

        if (!ts.expectKeyword(KeywordType.IMPORT, Msg.EXPECTED_IMPORT))
            return null

        ts.next()

        if (ts.matchOperator(OperatorType.MUL)) {
            ts.next()
            return ImportStmtNode(
                moduleName = moduleName,
                kind = ImportKind.Wildcard,
                pos = pos
            )
        }

        val identifiers = parser.parseIdsWithSeparatorOper(separator = OperatorType.COMMA)
            ?: return null

        return ImportStmtNode(
            moduleName = moduleName,
            kind = ImportKind.Named(symbols = identifiers),
            pos = pos
        )
    }

    private fun parseNamespaceStmt(): NamespaceStmtNode? {
        val pos = ts.next().pos

        val name = if (ts.peek() !is Token.LBrace) {
            val t = ts.peek()

            if (t is Token.Identifier) {
                ts.next()
                t.toIdentifierNode()
            } else {
                syntaxError(Msg.EXPECTED_IDENTIFIER, t.pos)
                null
            }
        } else null

        if (!ts.expect(Token.LBrace::class, Msg.EXPECTED_LBRACE))
            return null

        val body = parseBlock()

        return NamespaceStmtNode(
            name = name,
            body = body,
            pos = pos
        )
    }

    private fun parseConstructorStmt(): ConstructorDeclStmtNode {
        val pos = ts.next().pos

        var params: List<VarDeclStmtNode>? = null

        if (ts.match(Token.LParen::class))
            analiseParams(parser.parseArgsList()) { params = it }

        val body = parseBlock()

        return ConstructorDeclStmtNode(
            modifiers = null,
            params = params ?: emptyList(),
            body = body,
            pos = pos
        )
    }

    private fun parseDestructorStmt(): DestructorDeclStmtNode {
        val pos = ts.next().pos

        if (ts.match(Token.LParen::class)) {
            val args = parser.parseArgsList() // skipping

            if (args.isNotEmpty())
                syntaxError(Msg.CONSTRUCTORS_CANNOT_HAVE_PARAMS, pos)
        }

        val body = parseBlock()

        return DestructorDeclStmtNode(
            modifiers = null,
            body = body,
            pos = pos
        )
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
                    syntaxError(Msg.EXPECTED_TYPE_PARAM_NAME, expr.pos)
                    null
                }
            }

            if (typeName != null)
                datatypes.add(typeName)
        }

        return datatypes
    }

    private fun analiseTemplateList(exprList: List<ExprNode>?): TypeNameListNode? {
        if (exprList.isNullOrEmpty())
            return null

        val params: MutableList<TypeNameNode> = mutableListOf()

        exprList.forEach { expr ->
            val typeName: TypeNameNode? = when (expr) {
                is IdentifierNode -> {
                    TypeNameNode(
                        name = expr,
                        bound = null,
                        pos = expr.pos
                    )
                }

                is BinOpNode -> {
                    if (expr.operator == BinOpType.COLON) {

                        val identifier = if (expr.left is IdentifierNode) expr.left as IdentifierNode
                        else {
                            syntaxError(Msg.EXPECTED_TYPE_PARAM_NAME, expr.pos)
                            return@forEach
                        }

                        val datatype = analiseAsDatatype(expr.right, allowAsExpression = false)

                        if (datatype is DatatypeNode)
                            TypeNameNode(
                                name = identifier,
                                bound = datatype,
                                pos = expr.pos
                            )
                        else null
                    } else {
                        syntaxError(Msg.EXPECTED_TYPE_PARAM_NAME, expr.left.pos)
                        null
                    }
                }

                else -> {
                    syntaxError(Msg.EXPECTED_TYPE_PARAM_NAME, expr.pos)
                    null
                }
            }

            if (typeName != null)
                params.add(typeName)
        }

        return TypeNameListNode(params = params, pos = exprList.firstOrNull()?.pos ?: Pos())
    }

    private fun buildVarDeclHeader(
        header: ExprNode,
        pos: Pos = header.pos,
        isMutable: Boolean = true
    ): VarDeclStmtNode? {
        var name: IdentifierNode? = null
        var dataType: BaseDatatypeNode? = null
        var initializer: ExprNode? = null

        analiseHeader(
            header = header,
            errorMsg = Msg.EXPECTED_VAR_DECL,
            handleName = { name = it },
            handleTypeNames = { },
            handleParams = { },
            handleSuperType = { dataType = it },
            handleInitializer = {
                initializer = if (it is VoidNode) null else it
            }
        )

        return VarDeclStmtNode(
            modifiers = null,
            isMutable = isMutable,
            name = name ?: return null,
            dataType = dataType ?: AutoDatatypeNode(pos),
            initializer = initializer,
            pos = pos
        )
    }

    private fun buildFuncDeclStmt(
        header: ExprNode,
        typeNames: TypeNameListNode?,
        body: BlockNode?,
        pos: Pos
    ): FuncDeclStmtNode? {
        var name: IdentifierNode? = null
        var params: List<VarDeclStmtNode>? = null
        var returnType: BaseDatatypeNode? = null
        var initializerBody: ExprNode? = null

        analiseHeader(
            header = header,
            errorMsg = Msg.EXPECTED_FUNC_DECL,
            handleName = { name = it },
            handleTypeNames = {
                if (it != null)
                    syntaxError(Msg.TYPE_NAMES_MUST_BE_PLACES_BEFORE_FUNC_NAME, it.pos)
            },
            handleParams = { params = it },
            handleSuperType = {
                returnType =
                    if (it is AutoDatatypeNode) VoidDatatypeNode(it.pos)
                    else it
            },
            handleInitializer = { initializerBody = it }
        )

        val finalReturnType = returnType ?: if (body == null) AutoDatatypeNode(pos) else VoidDatatypeNode(pos)

        return FuncDeclStmtNode(
            modifiers = null,
            name = name ?: return null,
            params = params ?: emptyList(),
            typeNames = typeNames,
            returnType = finalReturnType,
            body = initializerBody?.wrapToBody() ?: body,
            pos = pos
        )
    }

    private fun buildClassStmt(header: ExprNode, body: BlockNode?, pos: Pos): ClassDeclStmtNode? {
        var name: IdentifierNode? = null
        var typeNames: TypeNameListNode? = null
        var superClass: BaseDatatypeNode? = null
        var primaryConstrParams: List<VarDeclStmtNode>? = null
        var initializerBody: ExprNode? = null

        analiseHeader(
            header = header,
            errorMsg = Msg.EXPECTED_CLASS_DECL,
            handleName = { name = it },
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
            superClass = superClass ?: VoidDatatypeNode(pos),
            body = initializerBody?.wrapToBody() ?: body,
            pos = pos
        )
    }

    private fun buildInterfaceStmt(header: ExprNode, body: BlockNode?, pos: Pos): InterfaceDeclStmtNode? {
        var name: IdentifierNode? = null
        var typeNames: TypeNameListNode? = null
        var superInterface: BaseDatatypeNode? = null
        var initializerBody: ExprNode? = null

        analiseHeader(
            header = header,
            errorMsg = Msg.EXPECTED_INTERFACE_DECL,
            handleName = { name = it },
            handleTypeNames = { typeNames = it },
            handleParams = {
                syntaxError(Msg.INTERFACES_CANNOT_HAVE_CONSTRUCTORS, header.pos)
            },
            handleSuperType = { superInterface = it },
            handleInitializer = { initializerBody = it }
        )

        return InterfaceDeclStmtNode(
            modifiers = null,
            name = name ?: return null,
            typeNames = typeNames,
            superInterface = superInterface ?: VoidDatatypeNode(pos),
            body = initializerBody?.wrapToBody() ?: body,
            pos = pos
        )
    }


    private fun analiseHeader(
        header: ExprNode,
        errorMsg: String,
        handleName: (IdentifierNode) -> Unit,
        handleTypeNames: (TypeNameListNode?) -> Unit,
        handleParams: (List<VarDeclStmtNode>?) -> Unit,
        handleSuperType: (BaseDatatypeNode?) -> Unit,
        handleInitializer: (ExprNode?) -> Unit
    ) {
        fun analiseSuperClass(expr: ExprNode) {
            handleSuperType(analiseAsDatatype(expr) as BaseDatatypeNode?)
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
                analiseNameNode(header, errorMsg, handleName)
            }

            is DatatypeNode -> {
                handleNameAndParams(header)
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
                        analiseSuperClass(header.right)
                    }

                    else -> {
                        syntaxError(errorMsg, header.pos)
                        return
                    }
                }
            }

            else -> {
                syntaxError(errorMsg, header.pos)
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
        val pos = ts.next().pos

        val typeNames = if (ts.matchOperator(OperatorType.LESS))
            analiseTemplateList(parser.parseTypenameList()) else null

        val header = parser.parseExpr(ctx = ParsingContext.FuncHeader)

        val body = parseBodyForDeclStmt()

        return buildFuncDeclStmt(header, typeNames, body, pos)
    }

    private fun parseClassStmt(): ClassDeclStmtNode? {
        val pos = ts.next().pos

        val header = parser.parseExpr(ctx = ParsingContext.Header)

        val body = parseBodyForDeclStmt()

        return buildClassStmt(
            header = header,
            body = body,
            pos = pos
        )
    }

    private fun parseInterfaceStmt(): InterfaceDeclStmtNode? {
        val pos = ts.next().pos
        val header = parser.parseExpr(ctx = ParsingContext.Header)
        val body = parseBodyForDeclStmt()

        return buildInterfaceStmt(
            header = header,
            body = body,
            pos = pos
        )
    }

    private fun parseVarDeclStmt(): VarDeclStmtNode? {
        val isMutable = ts.peek() isKeyword KeywordType.VAR
        val pos = ts.next().pos
        val expr = parser.parseExpr(ctx = ParsingContext.Header)

        return buildVarDeclHeader(
            header = expr,
            pos = pos,
            isMutable = isMutable
        )
    }

    private fun parseEnumStmt(): EnumDeclStmtNode? {
        val pos = ts.next().pos

        val nameToken = ts.peek()

        val enumName = if (nameToken is Token.Identifier) {
            ts.next()
            nameToken.toIdentifierNode()
        } else {
            syntaxError(Msg.NAME_EXPECTED, nameToken.pos)
            return null
        }

        if (!ts.match(Token.LBrace::class)) {
            syntaxError(Msg.UNEXPECTED_TOKEN, ts.pos)
            ts.skipUntil(Token.LBrace::class, Token.RBrace::class, Token.Keyword::class, Token.Identifier::class)
            return EnumDeclStmtNode(
                modifiers = null,
                name = enumName,
                body = BlockNode.EMPTY,
                pos = pos
            )
        }


        if (!ts.expect(Token.LBrace::class, Msg.EXPECTED_LBRACE))
            return EnumDeclStmtNode(
                modifiers = null,
                name = enumName,
                body = BlockNode.EMPTY,
                pos = pos
            )

        val items = parseBlock()

        return EnumDeclStmtNode(modifiers = null, name = enumName, body = items, pos = pos)
    }

    private fun analiseAsDatatype(expr: ExprNode, allowAsExpression: Boolean = false): ExprNode? {
        val datatype = expr.tryConvertToDatatype()

        if (datatype == null) {
            if (allowAsExpression) return expr

            syntaxError(Msg.EXPECTED_TYPE_NAME, expr.pos)
            return null
        }

        if (datatype !is DatatypeNode) return datatype

        var successful = true

        datatype.typeNames?.forEach { typeName ->
            successful = successful && analiseAsDatatype(typeName, true) != null
        }

        return if (successful) datatype else null
    }

    private fun analiseNameNode(expr: ExprNode, msg: String, handleName: (IdentifierNode) -> Unit) {
        if (expr !is IdentifierNode) {
            syntaxError(msg, expr.pos)
            return
        }

        handleName(expr)
    }

    private fun analiseParams(exprList: List<ExprNode>, handleParams: (List<VarDeclStmtNode>?) -> Unit) {
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
        handleName: (IdentifierNode) -> Unit,
        handleTypeNames: (TypeNameListNode?) -> Unit,
        handleParams: (List<VarDeclStmtNode>?) -> Unit
    ) {
        when (expr) {
            is IdentifierNode -> {
                analiseNameNode(expr, msg, handleName)
            }

            is FuncCallNode -> {
                analiseNameNode(expr.receiver, msg, handleName)
                handleTypeNames(analiseTemplateList(expr.typeNames))
                analiseParams(expr.args, handleParams)
            }

            is DatatypeNode -> {
                analiseNameNode(expr.identifier, msg, handleName)
                handleTypeNames(analiseTemplateList(expr.typeNames))
            }

            else -> {
                syntaxError(Msg.EXPECTED_FUNC_DECL, expr.pos)
            }
        }
    }

    private fun parseContinueStmt() = ContinueStmtNode(pos = ts.next().pos)

    private fun parseBreakStmt() = BreakStmtNode(pos = ts.next().pos)

    private fun parseDeclarationWithModifiers(): DeclStmtNode? {
        val modifiers = parseModifiers()
        val pos = ts.pos

        val stmt = parse()

        val stmtWithMod = when {
            stmt is DeclStmtNode -> {
                stmt.modifiers = modifiers
                stmt
            }

            else -> {
                syntaxError(Msg.EXPECTED_A_DECLARATION, pos)
                null
            }
        }

        return stmtWithMod
    }

    private fun parseModifiers(): ModifierSetNode {
        val pos = ts.pos

        val modifiers = mutableSetOf<ModifierNode>()

        while (true) {
            val t = ts.peek()

            if (t !is Token.Keyword)
                break

            val modifier = modifierMapper.toSecond(t) ?: break
//                syntaxError(Messages.INVALID_MODIFIER, t.pos)
//                ts.next()
//                continue

            if (modifiers.any { it::class == modifier::class }) {
                syntaxError(Msg.F_REPEATED_MODIFIER.format(modifier.keyword.value), t.pos)
                ts.next()
                continue
            }

            modifiers.add(modifier)
            ts.next()
        }

        return ModifierSetNode(nodes = modifiers, pos = pos)
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
        val pos = ts.pos
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
                    parseIfElseStmt().wrapToBody()
                }

                else -> null
            }

        return IfElseStmtNode(
            condition = condition,
            body = body,
            elseBody = elseBody,
            pos = pos
        )
    }

    private fun parseWhileStmt(): WhileStmtNode {
        val pos = ts.next().pos

        val (condition, body) = parseConditionAndBody()

        val elseBody = if (ts.peek() isKeyword KeywordType.ELSE) {
            ts.next()
            parseBlock()
        } else null

        return WhileStmtNode(
            condition = condition,
            body = body,
            elseBody = elseBody,
            pos = pos
        )
    }

    private fun parseDoWhileStmt(): DoWhileStmtNode {
        val pos = ts.next().pos

        val body = parseBlock()

        ts.expectKeyword(KeywordType.WHILE, Msg.EXPECTED_WHILE_AND_POST_CONDITION)
        ts.next()

        val condition = parser.parseExpr(ctx = ParsingContext.Condition)

        return DoWhileStmtNode(
            condition = condition,
            body = body,
            pos = pos
        )
    }

    private fun parseForLoopStmt(): ForLoopStmtNode {
        val pos = ts.next().pos

        val (condition, body) = parseConditionAndBody()

        return ForLoopStmtNode(
            condition = condition,
            body = body,
            pos = pos
        )
    }


    private fun parseElseEntryStmt(): ElseEntryNode? {
        val pos = ts.next().pos
        val t = ts.peek()

        if (t isNotOperator OperatorType.ARROW) {
            syntaxError(Msg.EXPECTED_ARROW_OPERATOR, t.pos)
            return null
        }

        ts.next()

        return ElseEntryNode(
            expr = parse(),
            pos = pos
        )
    }

    private fun parseMatchStmt(): MatchStmtNode {
        val pos = ts.pos
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

        return MatchStmtNode(
            target = target,
            body = body,
            pos = pos
        )
    }

    private fun parseReturnStmt(): ReturnStmtNode {
        val pos = ts.pos
        ts.next()

        val stmt = if (ts.matchSemicolonOrLinebreak())
            VoidNode
        else parse()

        return ReturnStmtNode(
            expr = stmt,
            pos = pos
        )
    }

    private fun syntaxError(msg: String, pos: Pos) =
        parser.syntaxError(msg = msg, pos = pos)

    private fun warning(msg: String, pos: Pos) =
        parser.warning(msg = msg, pos = pos)
}