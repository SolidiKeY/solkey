# Provenance: forked `key.core` code in `keyext.solidity.core`

`keyext.solidity.core` is not a thin extension — roughly 80% of its ~600 Java files are a
package-renamed hard fork of `key.core` (via the intermediate KeY-Rust fork), retargeted at the
fork's own `logic`/`proof`/`theory`/`strategy` types instead of KeY's Java-specific ones. Only
~135 files (~13k LOC, mostly under `program/`, `rule/metaconstruct/`, `parser/Solidity*`,
`program/parser/`) are original Solidity code.

## Why the copies cannot be de-duplicated

The copies type-check against the fork's own `org.key_project.solidity.logic.op.*`,
`solidity.proof.{Goal,Proof}`, `solidity.theory.IntLDT`, etc., while the `key.core` originals bind
the same roles to Java-specific classes (`de.uka.ilkd.key`-lineage `Goal`, `Services`,
`IntegerLDT`). The shared abstractions live one level down in `key.ncore` / `key.ncore.calculus`,
which both sides already use. Hoisting the remaining 5–20% deltas into those shared modules would
mean refactoring `key.core` itself, which is outside this fork's blast radius.

## Rule for near-verbatim files: diff against upstream, don't restyle

The files below differ from their `key.core` originals by only a few lines. Their one maintenance
advantage is that upstream fixes can be ported with a cheap diff. **Do not restyle, reformat, or
"clean up" these files** — every cosmetic change destroys that property. Port upstream changes by
hand instead.

Largest near-verbatim copies (diff vs. upstream ≤ ~20%):

- `strategy/IntegerStrategy.java` (~5% diff)
- `rule/matching/inst/GenericSortInstantiations.java` (~1%)
- `strategy/FOLStrategy.java` (~18%)
- `logic/NamespaceSet.java`
- `logic/sort/ParametricSortInstance.java`
- `strategy/termgenerator/RootsGenerator.java`
- `strategy/termgenerator/SuperTermGenerator.java`
- `strategy/quantifierHeuristics/LiteralsSmallerThanFeature.java`
- `proof/event/NodeReplacement.java`
- `proof/io/UrlRuleSource.java` (byte-identical)
- `parser/builder/FunctionPredicateBuilder.java`

plus ~80 further files that differ by ≤10 lines. When in doubt, diff a candidate file against the
same path (or basename) under `key.core/src/main/java/de/uka/ilkd/key/` before editing it.

## Known future work (out of scope for the readability pass)

- Merge `SolJSONParser`'s parser-local type caches with the `SolidityInfo` registry — a semantic
  ownership question, not a rename.
- Unify the two Solidity frontends (`parser/SolidityToKeyConverter` for ANTLR and
  `program/parser/SolJSONParser` for `solc --ast-compact-json`) behind a shared AST-builder
  abstraction; today they build the same 29 AST node types independently and are kept honest only
  by `BothParsersTest`.
