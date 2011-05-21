/*
 * Copyright 2008  Reg Whitton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.dev.eval;

import java.math.BigDecimal;
import java.math.MathContext;

abstract class Operator {
	/**
	 * End of string reached.
	 */
	static final Operator END = new Operator(-1, 0, null, null, null) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			throw new RuntimeException("END is a dummy operation");
		}
	};

	/**
	 * condition ? (expression if true) : (expression if false)
	 */
	static final Operator TERNARY = new Operator(0, 3, "?", null, null) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return (value1.signum() != 0) ? value2 : value3;
		}
	};
	/**
	 * &amp;&amp;
	 */
	static final Operator AND = new Operator(0, 2, "&&", Type.BOOLEAN, Type.BOOLEAN) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1.signum() != 0 && value2.signum() != 0 ? BigDecimal.ONE : BigDecimal.ZERO;
		}
	};
	/**
	 * ||
	 */
	static final Operator OR = new Operator(0, 2, "||", Type.BOOLEAN, Type.BOOLEAN) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1.signum() != 0 || value2.signum() != 0 ? BigDecimal.ONE : BigDecimal.ZERO;
		}
	};
	/**
	 * &gt;
	 */
	static final Operator GT = new Operator(1, 2, ">", Type.BOOLEAN, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1.compareTo(value2) > 0 ? BigDecimal.ONE : BigDecimal.ZERO;
		}
	};
	/**
	 * &gt;=
	 */
	static final Operator GE = new Operator(1, 2, ">=", Type.BOOLEAN, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1.compareTo(value2) >= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
		}
	};
	/**
	 * &lt;
	 */
	static final Operator LT = new Operator(1, 2, "<", Type.BOOLEAN, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1.compareTo(value2) < 0 ? BigDecimal.ONE : BigDecimal.ZERO;
		}
	};
	/**
	 * &lt;=
	 */
	static final Operator LE = new Operator(1, 2, "<=", Type.BOOLEAN, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1.compareTo(value2) <= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
		}
	};
	/**
	 * ==
	 */
	static final Operator EQ = new Operator(1, 2, "==", Type.BOOLEAN, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1.compareTo(value2) == 0 ? BigDecimal.ONE : BigDecimal.ZERO;
		}
	};
	/**
	 * != or &lt;&gt;
	 */
	static final Operator NE = new Operator(1, 2, "!=", Type.BOOLEAN, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1.compareTo(value2) != 0 ? BigDecimal.ONE : BigDecimal.ZERO;
		}
	};
	/**
	 * +
	 */
	static final Operator ADD = new Operator(2, 2, "+", Type.ARITHMETIC, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1.add(value2);
		}
	};
	/**
	 * -
	 */
	static final Operator SUB = new Operator(2, 2, "-", Type.ARITHMETIC, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1.subtract(value2);
		}
	};
	/**
	 * /
	 */
	static final Operator DIV = new Operator(3, 2, "/", Type.ARITHMETIC, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1.divide(value2, MathContext.DECIMAL128);
		}
	};
	/**
	 * %
	 */
	static final Operator REMAINDER = new Operator(3, 2, "%", Type.ARITHMETIC, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1.remainder(value2, MathContext.DECIMAL128);
		}
	};
	/**
	 * *
	 */
	static final Operator MUL = new Operator(3, 2, "*", Type.ARITHMETIC, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1.multiply(value2);
		}
	};
	/**
	 * -negate
	 */
	static final Operator NEG = new Operator(4, 1, "-", Type.ARITHMETIC, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1.negate();
		}
	};
	/**
	 * +plus
	 */
	static final Operator PLUS = new Operator(4, 1, "+", Type.ARITHMETIC, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1;
		}
	};
	/**
	 * abs
	 */
	static final Operator ABS = new Operator(4, 1, " abs ", Type.ARITHMETIC, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1.abs();
		}
	};
	/**
	 * pow
	 */
	static final Operator POW = new Operator(4, 2, " pow ", Type.ARITHMETIC, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			try {
				return value1.pow(value2.intValueExact());
			} catch (ArithmeticException ae) {
				throw new RuntimeException("pow argument: " + ae.getMessage());
			}
		}
	};
	/**
	 * int
	 */
	static final Operator INT = new Operator(4, 1, "int ", Type.ARITHMETIC, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return new BigDecimal(value1.toBigInteger());
		}
	};
	/**
	 * No operation - used internally when expression contains only a reference to a variable.
	 */
	static final Operator NOP = new Operator(4, 1, "", Type.ARITHMETIC, Type.ARITHMETIC) {

		@Override
		BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
			return value1;
		}
	};

	final int precedence;
	final int numberOfOperands;
	final String string;
	final Type resultType;
	final Type operandType;

	Operator(final int precedence, final int numberOfOperands, final String string, final Type resultType, final Type operandType) {
		this.precedence = precedence;
		this.numberOfOperands = numberOfOperands;
		this.string = string;
		this.resultType = resultType;
		this.operandType = operandType;
	}

	abstract BigDecimal perform(BigDecimal value1, BigDecimal value2, BigDecimal value3);
}
