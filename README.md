# diff-view

[Unified diff/patch](https://www.gnu.org/software/diffutils/manual/html_node/Unified-Format.html)
viewer.  Trying to create a tool I often desire... to my liking.

General features include:

-   Colored output with diff hunk line numbering ruler;
-   Compact (path-folding) file outline with filtering.

## Background

For local diff/patch file viewing I've been using the
[TortoiseUDiff](https://tortoisesvn.net/docs/release/TortoiseSVN_en/tsvn-automation-udiff.html)
utility from the TortoiseSVN package as it provides useful color highlighting.
For larger diffs of many files I'm often missing an outline
(tree) of the files like (much late) Github's
[Pull Request File Tree](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/reviewing-changes-in-pull-requests/filtering-files-in-a-pull-request#using-the-file-tree).

## Work-in-progress

-   Fuzzy outline filter;
-   Word-diff highlighting;
-   Side-by-side diff view;
-   Ignore white space changes;
-   Explore if [GraalVM](https://www.graalvm.org/) binaries could be small
    enough to be considered for distribution.

Possible further development:

-   Get a diff from local `git` workspace to enable stuff like dynamic
    expansion of context;
    -   Maybe blame annotations.

Other software with similar features:

-   [ymattw/ydiff](https://github.com/ymattw/ydiff)
-   [jesseduffield/lazygit](https://github.com/jesseduffield/lazygit)
-   [git-diff-blame](https://github.com/stanio/git-diff-blame)
