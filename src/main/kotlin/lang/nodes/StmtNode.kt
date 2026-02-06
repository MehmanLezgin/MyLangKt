package lang.nodes

import lang.core.SourceRange

interface StmtNode : ExprNode

abstract class StmtNodeNoChild(
    override val range: SourceRange
) : ExprNode {
    override fun mapRecursive(mapper: NodeTransformFunc) = mapper(this)
}

data class IfElseStmtNode(
    val condition: ExprNode,
    val body: BlockNode,
    val elseBody: BlockNode?,
    override val range: SourceRange
) : StmtNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            condition = condition.mapRecursive(mapper),
            body = body.mapRecursive(mapper) as? BlockNode ?: body,
            elseBody = elseBody?.mapRecursive(mapper) as? BlockNode ?: elseBody
        )
        return mapper(newNode)
    }
}

data class MatchStmtNode(
    val target: ExprNode?,
    val body: BlockNode,
    override val range: SourceRange
) : StmtNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            target = target?.mapRecursive(mapper),
            body = body.mapRecursive(mapper) as? BlockNode ?: body
        )
        return mapper(newNode)
    }
}

data class ElseEntryNode(
    val expr: ExprNode,
    override val range: SourceRange
) : StmtNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            expr = expr.mapRecursive(mapper)
        )
        return mapper(newNode)
    }
}

data class WhileStmtNode(
    val condition: ExprNode,
    val body: BlockNode,
    val elseBody: BlockNode?,
    override val range: SourceRange
) : StmtNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            condition = condition.mapRecursive(mapper),
            body = body.mapRecursive(mapper) as? BlockNode ?: body,
            elseBody = elseBody?.mapRecursive(mapper) as? BlockNode ?: elseBody
        )
        return mapper(newNode)
    }
}

data class DoWhileStmtNode(
    val condition: ExprNode,
    val body: BlockNode,
    override val range: SourceRange
) : StmtNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            condition = condition.mapRecursive(mapper),
            body = body.mapRecursive(mapper) as? BlockNode ?: body
        )
        return mapper(newNode)
    }
}

data class ForLoopStmtNode(
    val condition: ExprNode,
    val body: BlockNode,
    override val range: SourceRange
) : StmtNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            condition = condition.mapRecursive(mapper),
            body = body.mapRecursive(mapper) as? BlockNode ?: body
        )
        return mapper(newNode)
    }
}

data class ReturnStmtNode(
    val expr: ExprNode,
    override val range: SourceRange
) : StmtNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            expr = expr.mapRecursive(mapper)
        )
        return mapper(newNode)
    }
}

open class DeclStmtNode<T: ExprNode>(
    open var modifiers: ModifierSetNode?,
    override val range: SourceRange,
    open val name: T? = null,
) : StmtNode {
    override fun mapRecursive(mapper: NodeTransformFunc) = mapper(this)
}

data class VarDeclStmtNode(
    override var modifiers: ModifierSetNode?,
    override val name: IdentifierNode,
    val dataType: BaseDatatypeNode,
    val initializer: ExprNode?,
    override val range: SourceRange,
    val isMutable: Boolean
) : DeclStmtNode<IdentifierNode>(modifiers, range, name) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            name = name.mapRecursive(mapper) as? IdentifierNode ?: name,
            dataType = dataType.mapRecursive(mapper) as? BaseDatatypeNode ?: dataType,
            initializer = initializer?.mapRecursive(mapper)
        )
        return mapper(newNode)
    }
}

open class FuncDeclStmtNode(
    override var modifiers: ModifierSetNode?,
    override val name: ExprNode,
    val typeNames: TypeNameListNode?,
    open val params: List<VarDeclStmtNode>,
    val returnType: BaseDatatypeNode,
    open val body: BlockNode?,
    override val range: SourceRange
) : DeclStmtNode<ExprNode>(modifiers, range, name) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = FuncDeclStmtNode(
            name = name.mapRecursive(mapper) as? IdentifierNode ?: name,
            typeNames = typeNames?.mapRecursive(mapper) as? TypeNameListNode ?: typeNames,
            returnType = returnType.mapRecursive(mapper) as? BaseDatatypeNode ?: returnType,
            body = body?.mapRecursive(mapper) as? BlockNode ?: body,
            modifiers = modifiers,
            params = params,
            range = range,
        )
        return mapper(newNode)
    }
}

data class ConstructorDeclStmtNode(
    override var modifiers: ModifierSetNode?,
    override val params: List<VarDeclStmtNode>,
    override val body: BlockNode?,
    override val range: SourceRange
) : FuncDeclStmtNode(
    modifiers = modifiers,
    name = IdentifierNode(value = getName(), range = range),
    typeNames = null,
    params = params,
    returnType = VoidDatatypeNode(range),
    body = body,
    range = range,
) {
    companion object {
        fun getName() = "\$constructor"
    }

    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            modifiers = (modifiers?.mapRecursive(mapper) as? ModifierSetNode? ?: modifiers),
            params = params.map { it.mapRecursive(mapper) as? VarDeclStmtNode ?: it },
            body = body?.mapRecursive(mapper) as? BlockNode ?: body,
        )
        return mapper(newNode)
    }
}

data class DestructorDeclStmtNode(
    override var modifiers: ModifierSetNode?,
    override val body: BlockNode?,
    override val range: SourceRange
) : FuncDeclStmtNode(
    modifiers = modifiers,
    name = IdentifierNode(value = getName(), range = range),
    typeNames = null,
    params = emptyList(),
    returnType = VoidDatatypeNode(range),
    body = body,
    range = range,
) {
    companion object {
        fun getName() = "\$destructor"
    }

    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            modifiers = modifiers?.mapRecursive(mapper) as? ModifierSetNode ?: modifiers,
            body = body?.mapRecursive(mapper) as? BlockNode ?: body
        )
        return mapper(newNode)
    }
}

/*data class ConstructorDeclStmtNode(
    override var modifiers: ModifierSetNode?,
    val params: List<VarDeclStmtNode>?,
    val body: BlockNode?,
    override val range: SourceRange
) : DeclStmtNode(modifiers, range) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            modifiers = (modifiers?.mapRecursive(mapper) as? ModifierSetNode? ?: modifiers) ,
            params = params?.map { it.mapRecursive(mapper) as? VarDeclStmtNode ?: it },
            body = body?.mapRecursive(mapper) as? BlockNode ?: body
        )
        return mapper(newNode)
    }
}*/


/*data class DestructorDeclStmtNode(
    override var modifiers: ModifierSetNode?,
    val body: BlockNode?,
    override val range: SourceRange
) : DeclStmtNode(modifiers, range) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            modifiers = modifiers?.mapRecursive(mapper) as? ModifierSetNode ?: modifiers,
            body = body?.mapRecursive(mapper) as? BlockNode ?: body
        )
        return mapper(newNode)
    }
}*/

data class InterfaceDeclStmtNode(
    override var modifiers: ModifierSetNode?,
    override val name: IdentifierNode,
    val typeNames: TypeNameListNode?,
    val superInterface: BaseDatatypeNode?,
    val body: BlockNode?,
    override val range: SourceRange
) : DeclStmtNode<IdentifierNode>(modifiers, range, name) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            name = name.mapRecursive(mapper) as? IdentifierNode ?: name,
            typeNames = typeNames?.mapRecursive(mapper) as? TypeNameListNode ?: typeNames,
            superInterface = superInterface?.mapRecursive(mapper) as? BaseDatatypeNode? ?: superInterface,
            body = body?.mapRecursive(mapper) as? BlockNode ?: body
        )
        return mapper(newNode)
    }
}

data class ClassDeclStmtNode(
    override var modifiers: ModifierSetNode?,
    override val name: IdentifierNode,
    val typeNames: TypeNameListNode?,
    val primaryConstrParams: List<VarDeclStmtNode>?,
    val superClass: BaseDatatypeNode?,
    val body: BlockNode?,
    override val range: SourceRange
) : DeclStmtNode<IdentifierNode>(modifiers, range, name) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            name = name.mapRecursive(mapper) as? IdentifierNode ?: name,
            typeNames = typeNames?.mapRecursive(mapper) as? TypeNameListNode ?: typeNames,
            primaryConstrParams = primaryConstrParams?.map { it.mapRecursive(mapper) as? VarDeclStmtNode ?: it },
            superClass = superClass?.mapRecursive(mapper) as? BaseDatatypeNode? ?: superClass,
            body = body?.mapRecursive(mapper) as? BlockNode ?: body
        )
        return mapper(newNode)
    }
}

data class EnumDeclStmtNode(
    override var modifiers: ModifierSetNode?,
    override val name: IdentifierNode,
    val body: BlockNode,
    override val range: SourceRange
) : DeclStmtNode<IdentifierNode>(modifiers, range, name) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            name = name.mapRecursive(mapper) as? IdentifierNode ?: name,
            body = body.mapRecursive(mapper) as? BlockNode ?: body
        )
        return mapper(newNode)
    }
}


data class ContinueStmtNode(
    override val range: SourceRange
) : StmtNodeNoChild(range)

data class BreakStmtNode(
    override val range: SourceRange
) : StmtNodeNoChild(range)

data class EnumItemNode(
    val name: IdentifierNode,
    val initializer: ExprNode?,
) : ExprNode {
    override val range: SourceRange = name.range

    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            name = name.mapRecursive(mapper) as? IdentifierNode ?: name,
            initializer = initializer?.mapRecursive(mapper)
        )
        return mapper(newNode)
    }
}

data class TryCatchStmtNode(
    val tryBody: BlockNode,
    val catchParam: ExprNode?,
    val catchBody: BlockNode?,
    val finallyBody: BlockNode?,
    override val range: SourceRange
) : StmtNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            tryBody = tryBody.mapRecursive(mapper) as? BlockNode ?: tryBody,
            catchParam = catchParam?.mapRecursive(mapper),
            catchBody = catchBody?.mapRecursive(mapper) as? BlockNode ?: catchBody,
            finallyBody = finallyBody?.mapRecursive(mapper) as? BlockNode ?: finallyBody
        )
        return mapper(newNode)
    }
}

data class NamespaceStmtNode(
    override val name: IdentifierNode?,
    val body: BlockNode,
    override val range: SourceRange
) : DeclStmtNode<IdentifierNode>(
    modifiers = null,
    name = name,
    range = range
) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            name = name?.mapRecursive(mapper) as? IdentifierNode ?: name,
            body = body.mapRecursive(mapper) as? BlockNode ?: body
        )
        return mapper(newNode)
    }
}

data class TypedefStmtNode(
    override val name: IdentifierNode,
    override val range: SourceRange,
    val dataType: BaseDatatypeNode
) : DeclStmtNode<IdentifierNode>(
    modifiers = null,
    name = name,
    range = range
) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            name = name.mapRecursive(mapper) as? IdentifierNode ?: name,
            dataType = dataType.mapRecursive(mapper) as? BaseDatatypeNode ?: dataType,
        )
        return mapper(newNode)
    }
}
