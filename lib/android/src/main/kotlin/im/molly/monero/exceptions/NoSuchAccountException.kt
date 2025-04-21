package im.molly.monero.exceptions

class NoSuchAccountException(private val accountIndex: Int) : NoSuchElementException() {
    override val message: String
        get() = "No account was found with the specified index: $accountIndex"
}
