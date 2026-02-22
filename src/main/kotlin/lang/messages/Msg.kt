package lang.messages

import lang.messages.Terms.ordinal

interface FormattableMsg

object Msg {
    const val SRC_CAN_CONTAIN_ONE_FILE_MODULE_DECL =
        "Source file can only contain one file-scoped module decl"

    const val INVALID_CONST_VALUE = "Invalid const value"
    const val CANNOT_EXPORT = "Cannot export"

    object VarMustBeInitialized : FormattableMsg {
        fun format(name: String) = "Variable '$name' must be initialized"
    }

    object AutoVarCannotBeInferred : FormattableMsg {
        fun format(name: String) =
            "Variable '${name}' has type 'auto' but type cannot be inferred. Specify type explicitly"
    }

    object SymbolIsNotRegistered : FormattableMsg {
        fun format(symName: String) =
            "a symbol '$symName' is not registered"
    }

    object SYMBOL_IS_INACCESSIBLE : FormattableMsg {
        fun format(symName: String) =
            "a symbol '$symName' is inaccessible"
    }

    object EXPECTED_X_NAME : FormattableMsg {
        fun format(itemKind: String) =
            "Expected $itemKind name"
    }

    object X_CANNOT_HAVE_Y : FormattableMsg {
        fun format(x: String, y: String) =
            "$x cannot have $y"
    }

    object CANNOT_FIND_DECLARATION_OF_SYM : FormattableMsg {
        fun format(name: String) =
            "Cannot find declaration of '$name'"
    }

    object F_MODULE_DOES_NOT_EXPORT_SYM : FormattableMsg {
        fun format(modName: String, symName: String) =
            "A module '$modName' does not export '$symName'"
    }

    object MODULE_ALREADY_EXISTS_IN : FormattableMsg {
        fun format(modName: String, symName: String) =
            "Module '$modName' already exists in '$symName'"
    }

    interface DirectoryMsg : FormattableMsg{
        fun format(path: String): String
    }

    object F_NO_SUCH_DIRECTORY : DirectoryMsg {
        override fun format(path: String) =
            "No such directory '$path'"
    }

    object F_NOT_A_DIRECTORY : DirectoryMsg {
        override fun format(path: String) =
            "Path '$path' is not a directory"
    }

    object CANNOT_OPEN_SOURCE_FILE : DirectoryMsg {
        override fun format(path: String) =
            "Cannot open source file: '$path'"
    }

    object F_SYM_NOT_ALLOWED_HERE : FormattableMsg {
        fun format(name: String) =
            "'$name' is not allowed here"
    }

    object F_MODIFIER_IS_NOT_ALLOWED_ON : FormattableMsg {
        fun format(modifierName: String, declKindName: String) =
            "Modifier '$modifierName' is not allowed on $declKindName"
    }

    object F_MODIFIER_IS_NOT_INAPPLICABLE_ON_THIS_X : FormattableMsg {
        fun format(modName: String, itemName: String) =
            "'$modName' modifier is inapplicable to this ${itemName}"
    }

    object F_MODIFIER_IS_INCOMPATIBLE_WITH : FormattableMsg {
        fun format(modifierName: String, declKindName: String) =
            "Modifier '$modifierName' is incompatible with '$declKindName'"
    }

    object F_MUST_BE_OPEN_TYPE : FormattableMsg {
        fun format(declKindName: String) =
            "'$declKindName' must be declared as open"
    }

    object F_NONE_OF_N_CANDIDATES_APPLICABLE_FOR_TYPE : FormattableMsg {
        fun format(count: Int, typeName: String) =
            "None of the $count candidates is applicable for type '$typeName'"
    }


    object NoValuePassedForParameter : FormattableMsg {
        fun format(paramName: String, paramType: String) =
            "No value passed for parameter '$paramName' with type '$paramType'"

        fun format(paramIndex: Int, paramType: String) =
            "No value passed for ${paramIndex.ordinal()} parameter with type '$paramType'"

    }



    object MismatchExpectedActual : FormattableMsg {
        fun format(mismatchKind: String, typeName1: String, typeName2: String) =
            "$mismatchKind mismatch: expected '$typeName1', actual '$typeName2'"
    }

    object CannotCastType : FormattableMsg {
        fun format(typeName1: String, typeName2: String) =
            "Cannot cast type '$typeName1' to '$typeName2'"
    }

    interface NoBaseOperOverload : FormattableMsg {
        fun format(funcName: String, paramsStr: String, scopeName: String?): String
    }

    object CannotRegisterModule {
        fun format(moduleName: String) =
            "cannot register a module '$moduleName'"
    }


    object NoOperOverload : NoBaseOperOverload {
        override fun format(funcName: String, paramsStr: String, scopeName: String?) =
            SymbolNotDefinedIn.format(
                Terms.OPERATOR,
                "$funcName($paramsStr)",
                scopeName
            )
    }

    object NoFuncOverload : NoBaseOperOverload {
        override fun format(funcName: String, paramsStr: String, scopeName: String?) =
            SymbolNotDefinedIn.format(
                Terms.FUNCTION,
                "$funcName($paramsStr)",
                scopeName
            )
    }

    object SymRequiresItem : FormattableMsg {
        fun format(operKind: String, symName: String, requiredItemName: String) =
            "$operKind '$symName' requires $requiredItemName"
    }

    object SymbolNotDefinedIn : FormattableMsg {
        fun format(itemKind: String = Terms.SYMBOL, name: String, scopeName: String?) =
            "$itemKind '$name' not defined in ${scopeName ?: Terms.CURRENT_SCOPE}"
    }

    object SymbolAlreadyDefinedIn : FormattableMsg {
        fun format(itemKind: String = Terms.SYMBOL, name: String, scopeName: String?) =
            "$itemKind '$name' is already defined in ${scopeName ?: Terms.CURRENT_SCOPE}"
    }

    object RepeatedModifier : FormattableMsg {
        fun format(modifierName: String) =
            "Repeated modifier '$modifierName'"
    }

    const val MODULE_CANNOT_IMPORT_ITSELF = "A module cannot import itself"
    const val IMPORT_CYCLE_NOT_ALLOWED = "Import cycle not allowed"
    const val MODULE_NOT_DEFINED = "Module is not defined"
    const val ENTRY_SOURCE_NOT_DEFINED = "Entry source not defined"
    const val EXPECTED_IMPORT = "Expected 'import'"
    const val MODULE_IS_NOT_AT_START = "Module declaration is not at the start of the file"
    const val EXPORT_IS_NOT_ALLOWED_IN_THIS_SCOPE = "Modifier 'export' is not allowed in this scope"
    const val STATIC_IS_NOT_ALLOWED_IN_THIS_SCOPE = "Modifier 'static' is not allowed in this scope"
    const val VOID_CANNOT_BE_PARAM_TYPE = "'void' cannot be a parameter type"
    const val STATIC_FUNC_CANNOT_BE_ABSTRACT = "Static function cannot be abstract"
    const val STATIC_FUNC_CANNOT_BE_OPEN = "Static function cannot be open"
    const val STATIC_FUNC_CANNOT_BE_OVERRIDDEN = "Static function cannot override another function"
    const val SYM_NOT_A_FUNC = "Symbol is not a function"
    const val CONST_VAR_MUST_BE_STATIC = "Const variable must also be static"

    const val OVERRIDE_ALLOWED_ONLY_IN_CLASS_SCOPE = "Override is allowed only in a class scope"
    const val OVERRIDE_MEMBER_MUST_BE_PUBLIC = "Overriding member must be public"

    const val INTERFACE_CAN_EXTEND_INTERFACE = "An interface can only extend another interface"
    const val CLASS_CAN_EXTEND_INTERFACE_OR_CLASS = "A class can only extend an interface or another class"
    const val EXPECTED_VALUE_OR_REF = "Expected a value or reference"
    const val EXPECTED_MODULE_NAME = "Expected a module name"
    const val AMBIGUOUS_OVERLOADED_FUNCTION = "Ambiguous overloaded function"
    const val EXPECTED_VARIABLE_ACTUAL_VALUE = "Expected a variable, actual: a value"
    const val EXPECTED_A_POINTER_VALUE = "Expected a pointer value"
    const val EXPECTED_VARIABLE_ACTUAL_TYPE_NAME = "Expected a variable, actual: a type name"
    const val ASSIGNMENT_TO_IMMUTABLE_VARIABLE = "Assignment to immutable variable"
    const val ASSIGNMENT_TO_CONSTANT_VARIABLE = "Assignment to constant variable"
    const val UNRESOLVED_TYPE = "[Unresolved type]"
    const val ERROR_TYPE = "[Error type]"
    const val CONST = "const"
    const val EXPECTED_CONST_VALUE = "Expected const value"
    const val CONSTRUCTOR_OUTSIDE_CLASS_ERROR = "Constructor outside class body is not allowed"
    const val DESTRUCTOR_OUTSIDE_CLASS_ERROR = "Destructor outside class body is not allowed"
    const val EXPECTED_OPERATOR = "Expected an operator"
    const val CONFLICTING_OVERLOADS = "Conflicting overloads"
    const val REDECLARATION = "Redeclaration"
    const val CANNOT_RESOLVE_PARAM_OUTSIDE_FUNC = "Cannot resolve parameter outside a function"
    const val EXPECTED_ASSIGN: String = "Expected '='"
    const val LITERALS_MUST_BE_SURROUNDED_BY_WHITESPACES = "Literals must be surrounded by whitespaces"
    const val INVALID_FLOAT_LITERAL = "Invalid float literal"
    const val INVALID_DOUBLE_LITERAL = "Invalid double literal"
    const val INVALID_INT_LITERAL = "Invalid integer literal"
    const val ILLEGAL_ESCAPE_SEQUENCE = "Illegal escape sequence"
    const val EXPECTED_TOP_LEVEL_DECL = "Expected a top-level declaration"
    const val CONSTRUCTORS_CANNOT_HAVE_PARAMS = "Constructors cannot have parameters"
    const val TYPE_NAMES_MUST_BE_PLACES_BEFORE_FUNC_NAME = "Type parameters must be placed before function name"
    const val EXPECTED_ARROW_OPERATOR = "Expected '->'"
    const val EXPECTED_INTERFACE_DECL = "Expected an interface declaration"
    const val EXPECTED_CLASS_DECL = "Expected a class declaration"
    const val EXPECTED_TYPE_PARAM_NAME = "Expected a type parameter name"
    const val INTERFACES_CANNOT_HAVE_CONSTRUCTORS = "Interfaces cannot have constructors."
    const val POINTER_TO_REFERENCE_IS_NOT_ALLOWED = "Pointer to reference is not allowed"
    const val REF_TO_REF_IS_NOT_ALLOWED = "Reference to reference ('&&') is not allowed"
    const val EXPECTED_AN_EXPRESSION = "Expected an expression"
    const val EXPECTED_VAR_DECL = "Expected a variable declaration"
    const val EXPECTED_FUNC_DECL = "Expected a function declaration"
    const val EXPECTED_LESS_OP = "Expected '<'"
    const val EXPECTED_GREATER_OP = "Expected '>'"
    const val EXPECTED_TYPE_NAME = "Expected type name"
    const val EXPECTED_QUOTE = "Expecting '\"'"
    const val EXPECTED_COMMENT_END = "Expecting '*/'"
    const val EXPECTED_TRY = "Expected 'try'"
    const val EXPECTED_CATCH = "Expected 'catch'"
    const val EXPECTED_A_DECLARATION = "Expected a declaration"
    const val EXPECTED_IF = "Expected 'if'"
    const val EXPECTED_LBRACE_AFTER_CONDITION = "Expected '{' after condition"
    const val EXPECTED_SEMICOLON: String = "Expected ';'"
    const val EXPECTED_WHILE_AND_POST_CONDITION = "Expected 'while' and post-condition"
    const val EXPECTED_RPAREN = "Expected ')'"
    const val EXPECTED_LBRACE = "Expected '{'"
    const val EXPECTED_RBRACE = "Expected '}'"
    const val EXPECTED_RBRACKET = "Expected ']'"
    const val EXPECTED_IDENTIFIER = "Expected an identifier"
    const val NAME_EXPECTED = "Expected a name"
    const val UNEXPECTED_TOKEN = "Unexpected token"
}