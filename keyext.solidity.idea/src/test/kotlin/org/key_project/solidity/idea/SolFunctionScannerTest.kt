package org.key_project.solidity.idea

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The scanner is the only part of the plugin testable without an IDE, and it is where the bugs
 * are: everything else is wiring.
 */
class SolFunctionScannerTest {

    private fun names(source: String) = SolFunctionScanner.scan(source).map { "${it.contract}.${it.name}" }

    @Test
    fun `finds public functions of every contract in a file`() {
        val source = """
            contract A {
                function one() public { }
                function two() public { }
            }
            contract B {
                function three() public { }
            }
        """.trimIndent()

        assertEquals(listOf("A.one", "A.two", "B.three"), names(source))
    }

    @Test
    fun `only public functions are offered`() {
        val source = """
            contract A {
                function pub() public { }
                function inter() internal { }
                function priv() private { }
                function ext() external { }
                function bare() { }
            }
        """.trimIndent()

        assertEquals(listOf("A.pub"), names(source))
    }

    /**
     * Parameter lists nest, so a `[^)]*` pattern would stop inside `mapping(...)` and read the
     * modifiers wrong.
     */
    @Test
    fun `nested parentheses in the parameter list do not end it`() {
        val source = """
            contract A {
                function f(mapping(uint => uint) storage m, uint x) public { }
            }
        """.trimIndent()

        assertEquals(listOf("A.f"), names(source))
    }

    /** `returns (uint)` and a modifier call both put parentheses in the modifier run. */
    @Test
    fun `parentheses after the parameter list do not hide the body`() {
        val source = """
            contract A {
                function f() public onlyOwner(msg.sender) returns (uint) { return 1; }
                function g() public view returns (uint, bool) { }
            }
        """.trimIndent()

        assertEquals(listOf("A.f", "A.g"), names(source))
    }

    @Test
    fun `an interface stub without a body is not offered`() {
        val source = """
            interface I {
                function f() public;
                function g() external;
            }
            contract A {
                function h() public { }
            }
        """.trimIndent()

        assertEquals(listOf("A.h"), names(source))
    }

    @Test
    fun `commented out functions are ignored`() {
        val source = """
            contract A {
                // function commentedLine() public { }
                /* function commentedBlock() public { } */
                /** function commentedDoc() public { } */
                function real() public { }
            }
        """.trimIndent()

        assertEquals(listOf("A.real"), names(source))
    }

    @Test
    fun `the word function inside a string is ignored`() {
        val source = """
            contract A {
                function real() public {
                    string memory s = "function fake() public { }";
                    bytes memory b = 'function alsoFake() public { }';
                }
            }
        """.trimIndent()

        assertEquals(listOf("A.real"), names(source))
    }

    @Test
    fun `a function named like the public modifier is not confused with it`() {
        val source = """
            contract A {
                function publicThing() internal { }
                function publicOther() public { }
            }
        """.trimIndent()

        assertEquals(listOf("A.publicOther"), names(source))
    }

    @Test
    fun `libraries and interfaces are tracked as containers`() {
        val source = """
            library L {
                function f() public { }
            }
            contract A {
                function g() public { }
            }
        """.trimIndent()

        assertEquals(listOf("L.f", "A.g"), names(source))
    }

    @Test
    fun `a free function has no contract`() {
        val source = """
            function free() public { }
            contract A {
                function inside() public { }
            }
        """.trimIndent()

        assertEquals(listOf("null.free", "A.inside"), names(source))
    }

    /**
     * Braces in a function body must not pop the contract's frame, or every function after the
     * first would be reported as free.
     */
    @Test
    fun `braces inside a body do not lose the contract`() {
        val source = """
            contract A {
                function first() public {
                    if (true) { uint x = 1; } else { uint y = 2; }
                    for (uint i = 0; i < 3; i++) { }
                }
                function second() public { }
            }
        """.trimIndent()

        assertEquals(listOf("A.first", "A.second"), names(source))
    }

    /** Constructors, modifiers, `receive` and `fallback` are not `function` declarations. */
    @Test
    fun `constructors modifiers receive and fallback are not offered`() {
        val source = """
            contract A {
                constructor() public { }
                modifier onlyOwner() { _; }
                receive() external payable { }
                fallback() external payable { }
                function f() public { }
            }
        """.trimIndent()

        assertEquals(listOf("A.f"), names(source))
    }

    @Test
    fun `the offset points at the function keyword`() {
        val source = """
            contract A {
                function target() public { }
            }
        """.trimIndent()

        val found = SolFunctionScanner.scan(source).single()
        assertEquals(source.indexOf("function target"), found.offset)
    }

    /** A natspec comment before the declaration must not shift the offset onto the comment. */
    @Test
    fun `natspec before a function does not move the offset`() {
        val source = """
            contract A {
                /// @custom:key box
                /// Adds two numbers.
                function documented() public { }
            }
        """.trimIndent()

        val found = SolFunctionScanner.scan(source).single()
        assertEquals(source.indexOf("function documented"), found.offset)
    }

    /** The keyboard path resolves a caret to the function it sits in, body included. */
    @Test
    fun `the range spans the declaration and its body`() {
        val source = """
            contract A {
                function first() public { uint x = 1; }
                function second() public { }
            }
        """.trimIndent()

        val (first, second) = SolFunctionScanner.scan(source)
        assertTrue(source.indexOf("uint x") in first)
        assertTrue(source.indexOf("function first") in first)
        assertTrue(source.indexOf("function second") !in first)
        assertTrue(source.indexOf("function second") in second)
        assertEquals("}", source.substring(first.endOffset - 1, first.endOffset))
    }

    @Test
    fun `truncated input does not throw`() {
        assertTrue(SolFunctionScanner.scan("contract A { function f(").isEmpty())
        assertTrue(SolFunctionScanner.scan("contract A { function f() public").isEmpty())
        assertTrue(SolFunctionScanner.scan("/* unterminated").isEmpty())
        assertTrue(SolFunctionScanner.scan("\"unterminated").isEmpty())
        assertTrue(SolFunctionScanner.scan("").isEmpty())
        assertEquals(listOf("null.f"), names("function f() public { }"))
    }
}
