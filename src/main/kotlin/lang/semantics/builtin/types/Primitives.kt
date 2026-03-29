package lang.semantics.builtin.types

import lang.core.operators.OperatorType
import lang.messages.Terms
import lang.semantics.builtin.PrimitivesScope.float32
import lang.semantics.builtin.builders.constVar
import lang.semantics.builtin.builders.init
import lang.semantics.builtin.builders.operFunc
import lang.semantics.scopes.BaseTypeScope
import lang.semantics.types.PrimitiveFamily
import lang.semantics.types.PrimitiveSize
import lang.semantics.types.PrimitiveType
import lang.semantics.types.TypeFlags


class VoidPrimitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "void",
    primitiveSize = PrimitiveSize.NO_SIZE,
    family = PrimitiveFamily.VOID
) {
    override fun recreate(flags: TypeFlags) = VoidPrimitive(flags = flags)
}

class BoolPrimitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "bool",
    primitiveSize = PrimitiveSize.BYTE,
    family = PrimitiveFamily.BOOL
) {
    override fun recreate(flags: TypeFlags) = BoolPrimitive(flags = flags)
}

class CharPrimitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "char",
    primitiveSize = PrimitiveSize.BYTE,
    family = PrimitiveFamily.CHAR
) {
    override fun recreate(flags: TypeFlags) = CharPrimitive(flags = flags)

    override fun initWith(scope: BaseTypeScope) {
        super.initWith(scope)
        val charType = this

        scope.init {
            constVar(Terms.MIN_VALUE, charType, Char.MIN_VALUE)
            constVar(Terms.MAX_VALUE, charType, Char.MAX_VALUE)

            operFunc(OperatorType.PLUS) {
                params { Terms.OTHER ofType charType }
                returns(charType)
            }

            operFunc(OperatorType.MINUS) {
                params { Terms.OTHER ofType charType }
                returns(charType)
            }

            operFunc(OperatorType.MINUS) {
                returns(charType)
            }

            operFunc(OperatorType.INCREMENT) {
                returns(charType)
            }

            operFunc(OperatorType.DECREMENT) {
                returns(charType)
            }
        }
    }
}

class UCharPrimitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "uchar",
    primitiveSize = PrimitiveSize.BYTE,
    family = PrimitiveFamily.CHAR,
    signed = false
) {
    override fun recreate(flags: TypeFlags) = UCharPrimitive(flags = flags)

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun initWith(scope: BaseTypeScope) {
        super.initWith(scope)
        val type = this

        scope.init {
            constVar(Terms.MIN_VALUE, type, UByte.MIN_VALUE)
            constVar(Terms.MAX_VALUE, type, UByte.MAX_VALUE)
        }
    }
}

class Int8Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "byte",
    primitiveSize = PrimitiveSize.BYTE,
    family = PrimitiveFamily.INT
) {
    override fun recreate(flags: TypeFlags) = Int8Primitive(flags = flags)

    override fun initWith(scope: BaseTypeScope) {
        super.initWith(scope)
        val type = this

        scope.init {
            constVar(Terms.MIN_VALUE, type, Byte.MIN_VALUE)
            constVar(Terms.MAX_VALUE, type, Byte.MAX_VALUE)
        }
    }
}

class UInt8Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "ubyte",
    primitiveSize = PrimitiveSize.BYTE,
    family = PrimitiveFamily.UINT,
    signed = false
) {
    override fun recreate(flags: TypeFlags) = UInt8Primitive(flags = flags)

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun initWith(scope: BaseTypeScope) {
        super.initWith(scope)
        val type = this

        scope.init {
            constVar(Terms.MIN_VALUE, type, UByte.MIN_VALUE)
            constVar(Terms.MAX_VALUE, type, UByte.MAX_VALUE)
        }
    }
}

class Int16Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "short",
    primitiveSize = PrimitiveSize.WORD,
    family = PrimitiveFamily.INT
) {
    override fun recreate(flags: TypeFlags) = Int16Primitive(flags = flags)

    override fun initWith(scope: BaseTypeScope) {
        super.initWith(scope)
        val type = this

        scope.init {
            constVar(Terms.MIN_VALUE, type, Short.MIN_VALUE)
            constVar(Terms.MAX_VALUE, type, Short.MAX_VALUE)
        }
    }
}

class UInt16Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "ushort",
    primitiveSize = PrimitiveSize.WORD,
    family = PrimitiveFamily.UINT,
    signed = false
) {
    override fun recreate(flags: TypeFlags) = UInt16Primitive(flags = flags)

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun initWith(scope: BaseTypeScope) {
        super.initWith(scope)
        val type = this

        scope.init {
            constVar(Terms.MIN_VALUE, type, UShort.MIN_VALUE)
            constVar(Terms.MAX_VALUE, type, UShort.MAX_VALUE)
        }
    }
}

class Int32Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "int",
    primitiveSize = PrimitiveSize.DWORD,
    family = PrimitiveFamily.INT
) {
    override fun recreate(flags: TypeFlags) = Int32Primitive(flags = flags)

    override fun initWith(scope: BaseTypeScope) {
        super.initWith(scope)
        val type = this

        scope.init {
            constVar(Terms.MIN_VALUE, type, Int.MIN_VALUE)
            constVar(Terms.MAX_VALUE, type, Int.MAX_VALUE)

            operFunc(OperatorType.PLUS) {
                params { Terms.OTHER ofType type }
                returns(type)
            }

            operFunc(OperatorType.MUL) {
                params { Terms.OTHER ofType type }
                returns(type)
            }

            operFunc(OperatorType.SHIFT_LEFT) {
                params { Terms.OTHER ofType type }
                returns(type)
            }

            operFunc(OperatorType.SHIFT_RIGHT) {
                params { Terms.OTHER ofType type }
                returns(type)
            }
        }
    }
}

class UInt32Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "uint",
    primitiveSize = PrimitiveSize.DWORD,
    family = PrimitiveFamily.UINT,
    signed = false
) {
    override fun recreate(flags: TypeFlags) = UInt32Primitive(flags = flags)

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun initWith(scope: BaseTypeScope) {
        super.initWith(scope)
        val type = this

        scope.init {
            constVar(Terms.MIN_VALUE, type, UInt.MIN_VALUE)
            constVar(Terms.MAX_VALUE, type, UInt.MAX_VALUE)
        }
    }
}

class Int64Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "long",
    primitiveSize = PrimitiveSize.QWORD,
    family = PrimitiveFamily.INT
) {
    override fun recreate(flags: TypeFlags) = Int64Primitive(flags = flags)

    override fun initWith(scope: BaseTypeScope) {
        super.initWith(scope)
        val type = this

        scope.init {
            constVar(Terms.MIN_VALUE, type, Long.MIN_VALUE)
            constVar(Terms.MAX_VALUE, type, Long.MAX_VALUE)
        }
    }
}

class UInt64Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "ulong",
    primitiveSize = PrimitiveSize.QWORD,
    family = PrimitiveFamily.UINT,
    signed = false
) {
    override fun recreate(flags: TypeFlags) = UInt64Primitive(flags = flags)

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun initWith(scope: BaseTypeScope) {
        super.initWith(scope)
        val type = this

        scope.init {
            constVar(Terms.MIN_VALUE, type, ULong.MIN_VALUE)
            constVar(Terms.MAX_VALUE, type, ULong.MAX_VALUE)
        }
    }
}

class Float32Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "float",
    primitiveSize = PrimitiveSize.DWORD,
    family = PrimitiveFamily.FLOAT
) {
    override fun recreate(flags: TypeFlags) = Float32Primitive(flags = flags)

    override fun initWith(scope: BaseTypeScope) {
        super.initWith(scope)
        val type = this

        scope.init {
            constVar(Terms.MIN_VALUE, type, Float.MIN_VALUE)
            constVar(Terms.MAX_VALUE, type, Float.MAX_VALUE)
        }
    }
}

class Float64Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "double",
    primitiveSize = PrimitiveSize.QWORD,
    family = PrimitiveFamily.FLOAT
) {
    override fun recreate(flags: TypeFlags) = Float64Primitive(flags = flags)

    override fun initWith(scope: BaseTypeScope) {
        super.initWith(scope)
        val type = this

        scope.init {
            constVar(Terms.MIN_VALUE, type, Double.MIN_VALUE)
            constVar(Terms.MAX_VALUE, type, Double.MAX_VALUE)
        }
    }
}