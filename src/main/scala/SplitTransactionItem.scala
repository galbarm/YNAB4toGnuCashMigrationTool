case class SplitTransactionItem(S: String, E: String, $: String) {

  assert(S.nonEmpty, s"Split transactions with transfers are not supported: $this")

}