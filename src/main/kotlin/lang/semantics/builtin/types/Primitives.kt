package lang.semantics.builtin.types

import lang.semantics.builtin.operFunc
import lang.semantics.builtin.staticConstVar
import lang.semantics.scopes.Scope
import lang.semantics.types.ConstValue
import lang.semantics.types.PrimitiveSize
import lang.semantics.types.PrimitiveType
import lang.semantics.types.TypeFlags
import lang.core.operators.OperatorType

private object SymNames {
    const val MIN_VALUE = "MIN_VALUE"
    const val MAX_VALUE = "MAX_VALUE"
    const val OTHER = "other"

}

class VoidPrimitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "void",
    size = PrimitiveSize.NO_SIZE,
    prec = Int.MIN_VALUE
) {
    override fun recreate(flags: TypeFlags) = VoidPrimitive(flags = flags)
}

class BoolPrimitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "bool",
    size = PrimitiveSize.BYTE,
    prec = 0
) {
    override fun recreate(flags: TypeFlags) = BoolPrimitive(flags = flags)
}

class CharPrimitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "char",
    size = PrimitiveSize.BYTE,
    prec = 1
) {
    override fun recreate(flags: TypeFlags) = CharPrimitive(flags = flags)

    override fun initWith(scope: Scope) {
        super.initWith(scope)
        val charType = this

        this.apply {
            staticConstVar(SymNames.MIN_VALUE, this, ConstValue(Char.MIN_VALUE))
            staticConstVar(SymNames.MAX_VALUE, this, ConstValue(Char.MAX_VALUE))

            operFunc(OperatorType.PLUS) {
                params { SymNames.OTHER ofType charType }
                returns(charType)
            }

            operFunc(OperatorType.MINUS) {
                params { SymNames.OTHER ofType charType }
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
    size = PrimitiveSize.BYTE,
    prec = 1
) {
    override fun recreate(flags: TypeFlags) = UCharPrimitive(flags = flags)

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun initWith(scope: Scope) {
        super.initWith(scope)
        this.staticConstVar(SymNames.MIN_VALUE, this, ConstValue(UByte.MIN_VALUE))
            .staticConstVar(SymNames.MAX_VALUE, this, ConstValue(UByte.MAX_VALUE))
    }
}

class Int8Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "byte",
    size = PrimitiveSize.BYTE,
    prec = 2
) {
    override fun recreate(flags: TypeFlags) = Int8Primitive(flags = flags)

    override fun initWith(scope: Scope) {
        super.initWith(scope)
        this.staticConstVar(SymNames.MIN_VALUE, this, ConstValue(Byte.MIN_VALUE))
            .staticConstVar(SymNames.MAX_VALUE, this, ConstValue(Byte.MAX_VALUE))
    }
}

class UInt8Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "ubyte",
    size = PrimitiveSize.BYTE,
    prec = 3
) {
    override fun recreate(flags: TypeFlags) = UInt8Primitive(flags = flags)

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun initWith(scope: Scope) {
        super.initWith(scope)
        this.staticConstVar(SymNames.MIN_VALUE, this, ConstValue(UByte.MIN_VALUE))
            .staticConstVar(SymNames.MAX_VALUE, this, ConstValue(UByte.MAX_VALUE))
    }
}

class Int16Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "short",
    size = PrimitiveSize.WORD,
    prec = 4
) {
    override fun recreate(flags: TypeFlags) = Int16Primitive(flags = flags)

    override fun initWith(scope: Scope) {
        super.initWith(scope)
        this.staticConstVar(SymNames.MIN_VALUE, this, ConstValue(Short.MIN_VALUE))
            .staticConstVar(SymNames.MAX_VALUE, this, ConstValue(Short.MAX_VALUE))
    }
}

class UInt16Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "ushort",
    size = PrimitiveSize.WORD,
    prec = 5
) {
    override fun recreate(flags: TypeFlags) = UInt16Primitive(flags = flags)

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun initWith(scope: Scope) {
        super.initWith(scope)
        this.staticConstVar(SymNames.MIN_VALUE, this, ConstValue(UShort.MIN_VALUE))
            .staticConstVar(SymNames.MAX_VALUE, this, ConstValue(UShort.MAX_VALUE))
    }
}

class Int32Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "int",
    size = PrimitiveSize.DWORD,
    prec = 6
) {
    override fun recreate(flags: TypeFlags) = Int32Primitive(flags = flags)

    override fun initWith(scope: Scope) {
        super.initWith(scope)
        val int32Type = this

        this.apply {
            staticConstVar(SymNames.MIN_VALUE, this, ConstValue(Int.MIN_VALUE))
            staticConstVar(SymNames.MAX_VALUE, this, ConstValue(Int.MAX_VALUE))

            operFunc(OperatorType.PLUS) {
                params { SymNames.OTHER ofType int32Type }
                returns(int32Type)
            }

            operFunc(OperatorType.MUL) {
                params { SymNames.OTHER ofType int32Type }
                returns(int32Type)
            }

            operFunc(OperatorType.SHIFT_LEFT) {
                params { SymNames.OTHER ofType int32Type }
                returns(int32Type)
            }

            operFunc(OperatorType.SHIFT_RIGHT) {
                params { SymNames.OTHER ofType int32Type }
                returns(int32Type)
            }
        }
    }
}

class UInt32Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "uint",
    size = PrimitiveSize.DWORD,
    prec = 7
) {
    override fun recreate(flags: TypeFlags) = UInt32Primitive(flags = flags)

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun initWith(scope: Scope) {
        super.initWith(scope)
        this.staticConstVar(SymNames.MIN_VALUE, this, ConstValue(UInt.MIN_VALUE))
            .staticConstVar(SymNames.MAX_VALUE, this, ConstValue(UInt.MAX_VALUE))
    }
}

class Int64Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "long",
    size = PrimitiveSize.QWORD,
    prec = 8
) {
    override fun recreate(flags: TypeFlags) = Int64Primitive(flags = flags)

    override fun initWith(scope: Scope) {
        super.initWith(scope)
        this.staticConstVar(SymNames.MIN_VALUE, this, ConstValue(Long.MIN_VALUE))
            .staticConstVar(SymNames.MAX_VALUE, this, ConstValue(Long.MAX_VALUE))
    }
}

class UInt64Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "ulong",
    size = PrimitiveSize.QWORD,
    prec = 9
) {
    override fun recreate(flags: TypeFlags) = UInt64Primitive(flags = flags)

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun initWith(scope: Scope) {
        super.initWith(scope)
        this.staticConstVar(SymNames.MIN_VALUE, this, ConstValue(ULong.MIN_VALUE))
            .staticConstVar(SymNames.MAX_VALUE, this, ConstValue(ULong.MAX_VALUE))
    }
}

class Float32Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "float",
    size = PrimitiveSize.DWORD,
    prec = 10
) {
    override fun recreate(flags: TypeFlags) = Float32Primitive(flags = flags)

    override fun initWith(scope: Scope) {
        super.initWith(scope)
        this.staticConstVar(SymNames.MIN_VALUE, this, ConstValue(Float.MIN_VALUE))
            .staticConstVar(SymNames.MAX_VALUE, this, ConstValue(Float.MAX_VALUE))
    }
}

class Float64Primitive(
    override var flags: TypeFlags = TypeFlags()
) : PrimitiveType(
    flags = flags,
    name = "double",
    size = PrimitiveSize.QWORD,
    prec = 11
) {
    override fun recreate(flags: TypeFlags) = Float64Primitive(flags = flags)

    override fun initWith(scope: Scope) {
        super.initWith(scope)
        this.staticConstVar(SymNames.MIN_VALUE, this, ConstValue(Double.MIN_VALUE))
            .staticConstVar(SymNames.MAX_VALUE, this, ConstValue(Double.MAX_VALUE))
    }
}