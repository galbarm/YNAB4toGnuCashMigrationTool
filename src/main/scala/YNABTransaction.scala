case class YNABTransaction(D: String, T: String, P: String, M: String, N: String, C: Option[String], L: Option[String], split: Seq[SplitTransactionItem]) {

  P match {
    case payee if payee.isEmpty => assert(L.exists(!_.startsWith("Income:Available")), s"If there is no payee, transaction must be an expense: $this")
    case payee if payee.startsWith("Transfer :") => assert(L.isEmpty, s"transfer transaction cannot have a category: $this")
    case _ => assert(L.exists(_.startsWith("Income:Available")), s"payee transaction must have an income category: $this")
  }

}