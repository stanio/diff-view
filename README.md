# diff-view

-   Would provide file tree outline for
    [unified diff](https://www.gnu.org/software/diffutils/manual/html_node/Unified-Format.html) files;
-   Could provide side-by-side diff view;
-   Explore if [GraalVM](https://www.graalvm.org/) binaries could be small
    enough to be considered for distribution.

## Background

For local diff/patch file viewing I've been using the
[TortoiseUDiff](https://tortoisesvn.net/docs/release/TortoiseSVN_en/tsvn-automation-udiff.html)
utility from the TortoiseSVN package as it provides useful coloring
highlighting.  For larger diffs of many files I'm often missing an outline
(tree) of the files like (much late) Github's
[Pull Request File Tree](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/reviewing-changes-in-pull-requests/filtering-files-in-a-pull-request#using-the-file-tree).

## Work-in-progress

-   File tree outline with quick filter and folding path segments
-   Text view with line numbers ruler
