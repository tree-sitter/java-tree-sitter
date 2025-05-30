$package = 'io.github.treesitter.jtreesitter.internal'
$output = "$($args[1])/generated-sources/jextract"
$lib = "$($args[0])/core/lib"

& jextract.ps1 `
    --include-struct TSInput `
    --include-struct TSInputEdit `
    --include-struct TSLogger `
    --include-struct TSNode `
    --include-struct TSParseOptions `
    --include-struct TSParseState `
    --include-struct TSPoint `
    --include-struct TSQueryCapture `
    --include-struct TSQueryCursorOptions `
    --include-struct TSQueryCursorState `
    --include-struct TSQueryMatch `
    --include-struct TSQueryPredicateStep `
    --include-struct TSQueryPredicateStepType `
    --include-struct TSLanguageMetadata `
    --include-struct TSRange `
    --include-struct TSTreeCursor `
    --include-function free `
    --include-function malloc `
    --include-function calloc `
    --include-function realloc `
    --include-function ts_set_allocator `
    --include-function ts_language_abi_version `
    --include-function ts_language_copy `
    --include-function ts_language_delete `
    --include-function ts_language_field_count `
    --include-function ts_language_field_id_for_name `
    --include-function ts_language_field_name_for_id `
    --include-function ts_language_metadata `
    --include-function ts_language_name `
    --include-function ts_language_next_state `
    --include-function ts_language_state_count `
    --include-function ts_language_subtypes `
    --include-function ts_language_supertypes `
    --include-function ts_language_symbol_count `
    --include-function ts_language_symbol_for_name `
    --include-function ts_language_symbol_name `
    --include-function ts_language_symbol_type `
    --include-function ts_lookahead_iterator_current_symbol `
    --include-function ts_lookahead_iterator_current_symbol_name `
    --include-function ts_lookahead_iterator_delete `
    --include-function ts_lookahead_iterator_language `
    --include-function ts_lookahead_iterator_new `
    --include-function ts_lookahead_iterator_next `
    --include-function ts_lookahead_iterator_reset `
    --include-function ts_lookahead_iterator_reset_state `
    --include-function ts_node_child `
    --include-function ts_node_child_by_field_id `
    --include-function ts_node_child_by_field_name `
    --include-function ts_node_child_with_descendant `
    --include-function ts_node_child_count `
    --include-function ts_node_descendant_count `
    --include-function ts_node_descendant_for_byte_range `
    --include-function ts_node_descendant_for_point_range `
    --include-function ts_node_edit `
    --include-function ts_node_end_byte `
    --include-function ts_node_end_point `
    --include-function ts_node_eq `
    --include-function ts_node_field_name_for_child `
    --include-function ts_node_field_name_for_named_child `
    --include-function ts_node_first_child_for_byte `
    --include-function ts_node_first_named_child_for_byte `
    --include-function ts_node_grammar_symbol `
    --include-function ts_node_grammar_type `
    --include-function ts_node_has_changes `
    --include-function ts_node_has_error `
    --include-function ts_node_is_error `
    --include-function ts_node_is_extra `
    --include-function ts_node_is_missing `
    --include-function ts_node_is_named `
    --include-function ts_node_is_null `
    --include-function ts_node_language `
    --include-function ts_node_named_child `
    --include-function ts_node_named_child_count `
    --include-function ts_node_named_descendant_for_byte_range `
    --include-function ts_node_named_descendant_for_point_range `
    --include-function ts_node_next_named_sibling `
    --include-function ts_node_next_parse_state `
    --include-function ts_node_next_sibling `
    --include-function ts_node_parent `
    --include-function ts_node_parse_state `
    --include-function ts_node_prev_named_sibling `
    --include-function ts_node_prev_sibling `
    --include-function ts_node_start_byte `
    --include-function ts_node_start_point `
    --include-function ts_node_string `
    --include-function ts_node_symbol `
    --include-function ts_node_type `
    --include-function ts_parser_cancellation_flag `
    --include-function ts_parser_delete `
    --include-function ts_parser_included_ranges `
    --include-function ts_parser_language `
    --include-function ts_parser_logger `
    --include-function ts_parser_new `
    --include-function ts_parser_parse `
    --include-function ts_parser_parse_string `
    --include-function ts_parser_parse_string_encoding `
    --include-function ts_parser_parse_with_options `
    --include-function ts_parser_print_dot_graphs `
    --include-function ts_parser_reset `
    --include-function ts_parser_set_cancellation_flag `
    --include-function ts_parser_set_included_ranges `
    --include-function ts_parser_set_language `
    --include-function ts_parser_set_logger `
    --include-function ts_parser_set_timeout_micros `
    --include-function ts_parser_timeout_micros `
    --include-function ts_query_capture_count `
    --include-function ts_query_capture_name_for_id `
    --include-function ts_query_capture_quantifier_for_id `
    --include-function ts_query_cursor_delete `
    --include-function ts_query_cursor_did_exceed_match_limit `
    --include-function ts_query_cursor_exec `
    --include-function ts_query_cursor_exec_with_options `
    --include-function ts_query_cursor_match_limit `
    --include-function ts_query_cursor_new `
    --include-function ts_query_cursor_next_capture `
    --include-function ts_query_cursor_next_match `
    --include-function ts_query_cursor_remove_match `
    --include-function ts_query_cursor_set_byte_range `
    --include-function ts_query_cursor_set_match_limit `
    --include-function ts_query_cursor_set_max_start_depth `
    --include-function ts_query_cursor_set_point_range `
    --include-function ts_query_cursor_set_timeout_micros `
    --include-function ts_query_cursor_timeout_micros `
    --include-function ts_query_delete `
    --include-function ts_query_disable_capture `
    --include-function ts_query_disable_pattern `
    --include-function ts_query_end_byte_for_pattern `
    --include-function ts_query_is_pattern_guaranteed_at_step `
    --include-function ts_query_is_pattern_non_local `
    --include-function ts_query_is_pattern_rooted `
    --include-function ts_query_new `
    --include-function ts_query_pattern_count `
    --include-function ts_query_predicates_for_pattern `
    --include-function ts_query_start_byte_for_pattern `
    --include-function ts_query_string_count `
    --include-function ts_query_string_value_for_id `
    --include-function ts_tree_copy `
    --include-function ts_tree_cursor_copy `
    --include-function ts_tree_cursor_current_depth `
    --include-function ts_tree_cursor_current_descendant_index `
    --include-function ts_tree_cursor_current_field_id `
    --include-function ts_tree_cursor_current_field_name `
    --include-function ts_tree_cursor_current_node `
    --include-function ts_tree_cursor_delete `
    --include-function ts_tree_cursor_goto_descendant `
    --include-function ts_tree_cursor_goto_first_child `
    --include-function ts_tree_cursor_goto_first_child_for_byte `
    --include-function ts_tree_cursor_goto_first_child_for_point `
    --include-function ts_tree_cursor_goto_last_child `
    --include-function ts_tree_cursor_goto_next_sibling `
    --include-function ts_tree_cursor_goto_parent `
    --include-function ts_tree_cursor_goto_previous_sibling `
    --include-function ts_tree_cursor_new `
    --include-function ts_tree_cursor_reset `
    --include-function ts_tree_cursor_reset_to `
    --include-function ts_tree_delete `
    --include-function ts_tree_edit `
    --include-function ts_tree_get_changed_ranges `
    --include-function ts_tree_included_ranges `
    --include-function ts_tree_language `
    --include-function ts_tree_print_dot_graph `
    --include-function ts_tree_root_node `
    --include-function ts_tree_root_node_with_offset `
    --include-constant TREE_SITTER_LANGUAGE_VERSION `
    --include-constant TREE_SITTER_MIN_COMPATIBLE_LANGUAGE_VERSION `
    --include-constant TSInputEncodingCustom `
    --include-constant TSInputEncodingUTF16BE `
    --include-constant TSInputEncodingUTF16LE `
    --include-constant TSInputEncodingUTF8 `
    --include-constant TSLogTypeLex `
    --include-constant TSLogTypeParse `
    --include-constant TSQuantifierOne `
    --include-constant TSQuantifierOneOrMore `
    --include-constant TSQuantifierZero `
    --include-constant TSQuantifierZeroOrMore `
    --include-constant TSQuantifierZeroOrOne `
    --include-constant TSQueryErrorCapture `
    --include-constant TSQueryErrorField `
    --include-constant TSQueryErrorLanguage `
    --include-constant TSQueryErrorNodeType `
    --include-constant TSQueryErrorNone `
    --include-constant TSQueryErrorStructure `
    --include-constant TSQueryErrorSyntax `
    --include-constant TSQueryPredicateStepTypeCapture `
    --include-constant TSQueryPredicateStepTypeDone `
    --include-constant TSQueryPredicateStepTypeString `
    --include-constant TSSymbolTypeAnonymous `
    --include-constant TSSymbolTypeAuxiliary `
    --include-constant TSSymbolTypeRegular `
    --include-constant TSSymbolTypeSupertype `
    --include-typedef DecodeFunction `
    --header-class-name TreeSitter `
    --output $output `
    -t $package `
    -l tree-sitter `
    -I "$lib/src" `
    -I "$lib/include" `
    -DTREE_SITTER_HIDE_SYMBOLS `
    "$lib/include/tree_sitter/api.h"
