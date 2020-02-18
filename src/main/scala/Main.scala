import java.io.File
import java.nio.file.{Files, Paths}
import org.apache.commons.text.TextStringBuilder
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._


object Main {

  val outputFilename = "ynab.qif"

  def main(args: Array[String]): Unit = {
    val dir = new File(args(0))
    println(s"Directory: $dir")

    val accountFiles = dir.listFiles((_, name) => name.toLowerCase().endsWith(".qif")).toSeq.filterNot(_.getName == outputFilename)

    val accounts = removeDuplicateTransfers(accountFiles.map(CreateAccount))
    val output = createQIF(accounts)

    val file = Paths.get(args(0), outputFilename)
    Files.write(file, output.getBytes)
    println(s"File ${file.getFileName} successfully created")
  }


  def CreateAccount(file: File): Account = {
    val accountName = parseAccountName(file)
    val ynabTransactions = parseYnabTransactions(file)

    val gnuCashTransactions = ynabTransactions.map(ynab => {
      val category =
        if (ynab.P.isEmpty) s"Expenses:${ynab.L.get}"
        else if (ynab.P.startsWith("Transfer : "))
          s"Assets:${ynab.P.substring(11)}"
        else
          s"Income:${ynab.P}"

      val splits = ynab.split.map(item => item.copy(S = s"Expenses:${item.S}"))

      GnuCashTransaction(ynab.D, ynab.T, ynab.M, ynab.N, ynab.C, category, splits)
    })

    Account(accountName, gnuCashTransactions)
  }


  def removeDuplicateTransfers(accounts: Seq[Account]): Seq[Account] = {
    accounts.foldLeft(List.empty[Account])((result, account) =>
      result :+ Account(account.name, account.transactions.filterNot(transaction => result.map(_.name).contains(transaction.L)))
    )
  }


  def parseAccountName(file: File): String = {
    val name = file.getName
    val start = name.indexOf("-") + 1
    val end = name.indexOf(" as of")
    s"Assets:${name.substring(start, end)}"
  }


  def parseYnabTransactions(file: File): Seq[YNABTransaction] = {
    val fileContent = Files.readAllLines(file.toPath).asScala.tail.toSeq
    val transactions = splitBySeparator(fileContent, "^").filter(_.nonEmpty)

    transactions.map(transaction => {
      try {
        val date = transaction.find(_.startsWith("D")).get.substring(1)
        val total = transaction.find(_.startsWith("T")).get.substring(1)
        val payee = transaction.find(_.startsWith("P")).get.substring(1)
        val memo = transaction.find(_.startsWith("M")).get.substring(1)
        val num = transaction.find(_.startsWith("N")).get.substring(1)
        val cleared = transaction.find(_.startsWith("C")).map(_.substring(1))
        val category = transaction.find(_.startsWith("L")).map(_.substring(1))

        val splitCategories = transaction.filter(_.startsWith("S")).map(_.substring(1))
        val splitMemos = transaction.filter(_.startsWith("E")).map(_.substring(1))
        val splitTotal = transaction.filter(_.startsWith("$")).map(_.substring(1))
        val splits = (splitCategories, splitMemos, splitTotal).zipped.toSeq.map(split => SplitTransactionItem(split._1, split._2, split._3))

        YNABTransaction(date, total, payee, memo, num, cleared, category, splits)
      } catch {
        case ex: Throwable =>
          println(s"error while parsing transaction: $transaction")
          throw ex
      }
    })
  }


  def createQIF(accounts: Seq[Account]): String = {
    val builder = new TextStringBuilder()
    accounts.foreach(account => {
      builder.appendln("!Account")
      builder.appendln(s"N${account.name}")
      builder.appendln("^")
      builder.appendln("!Type:Bank")
      account.transactions.foreach(transaction => {
        builder.appendln(s"D${transaction.D}")
        builder.appendln(s"T${transaction.T}")
        builder.appendln(s"M${transaction.M}")
        builder.appendln(s"N${transaction.N}")
        transaction.C.foreach(c => builder.appendln(s"C$c"))
        builder.appendln(s"L${transaction.L}")
        transaction.split.foreach(splitItem => {
          builder.appendln(s"S${splitItem.S}")
          builder.appendln(s"E${splitItem.E}")
          builder.appendln("$" + splitItem.$)
        })
        builder.appendln("^")
      })
    })

    builder.toString
  }


  def splitBySeparator[T](l: Seq[T], sep: T): Seq[Seq[T]] = {
    val b = ListBuffer(ListBuffer[T]())
    l foreach { e =>
      if (e == sep) {
        if (b.last.nonEmpty) b += ListBuffer[T]()
      }
      else b.last += e
    }
    b.map(_.toSeq).toSeq
  }
}