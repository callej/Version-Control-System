package svcs

import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

const val PATH_SEPARATOR = '\\'
const val VCS_DIRECTORY = "vcs"
const val COMMIT_DIRECTORY = "commits"
const val CONFIG_FILE = "config.txt"
const val INDEX_FILE = "index.txt"
const val LOG_FILE = "log.txt"
const val HASH_FUNCTION = "SHA-256"
const val COMMAND_SPACE = 11

enum class Commands(val command: String, val description: String, val action: (Commands, List<String>) -> List<String> ) {
    CONFIG("config", "Get and set a username.", {cmd, args -> executeConfig(cmd, args)}),
    ADD("add", "Add a file to the index.", {cmd, args -> executeAdd(cmd, args)}),
    LOG("log", "Show commit logs.", {cmd, args -> executeLog(cmd, args)}),
    COMMIT("commit", "Save changes.", {cmd, args -> executeCommit(cmd, args)}),
    CHECKOUT("checkout", "Restore a file.", {cmd, args -> executeCheckout(cmd, args)})
}

fun String.hash(): String {
    val md = MessageDigest.getInstance(HASH_FUNCTION)
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}

fun createVCSDirectory() {
    if (!File(VCS_DIRECTORY).exists()) {
        File(VCS_DIRECTORY + PATH_SEPARATOR + COMMIT_DIRECTORY).mkdirs()
    }
}

fun executeConfig(cmd: Commands, args: List<String>): List<String> {
    if (args.isEmpty()) {
        if (File(VCS_DIRECTORY + PATH_SEPARATOR + CONFIG_FILE).exists()) {
            println("The username is ${File(VCS_DIRECTORY + PATH_SEPARATOR + CONFIG_FILE).readText()}.")
        }
        else {
            println("Please, tell me who you are.")
        }
    }
    else {
        File(VCS_DIRECTORY + PATH_SEPARATOR + CONFIG_FILE).writeText(args.first())
        println("The username is ${File(VCS_DIRECTORY + PATH_SEPARATOR + CONFIG_FILE).readText()}.")
    }
    return emptyList()
}

fun executeAdd(cmd: Commands, args: List<String>): List<String> {
    if (args.isEmpty()) {
        if (File(VCS_DIRECTORY + PATH_SEPARATOR + INDEX_FILE).exists()) {
            println("Tracked files:")
            println(File(VCS_DIRECTORY + PATH_SEPARATOR + INDEX_FILE).readText().dropLast(1))
        }
        else {
            println("Add a file to the index.")
        }
    }
    else {
        if (File(args.first()).exists()) {
            if (File(VCS_DIRECTORY + PATH_SEPARATOR + INDEX_FILE).exists()) {
                if (!File(VCS_DIRECTORY + PATH_SEPARATOR + INDEX_FILE).readText().contains(args.first())) {
                    File(VCS_DIRECTORY + PATH_SEPARATOR + INDEX_FILE).appendText(args.first() + "\n")
                }
            }
            else {
                File(VCS_DIRECTORY + PATH_SEPARATOR + INDEX_FILE).writeText(args.first() + "\n")
            }
            println("The file '${args.first()}' is tracked.")
        }
        else {
            println("Can't find '${args.first()}'.")
        }
    }
    return emptyList()
}

fun executeLog(cmd: Commands, args: List<String>): List<String> {
    if (File(VCS_DIRECTORY + PATH_SEPARATOR + LOG_FILE).exists()) {
        println(File(VCS_DIRECTORY + PATH_SEPARATOR + LOG_FILE).readText().dropLast(2))
    }
    else {
        println("No commits yet.")
    }
    return args
}

fun getHashValue(): String {
    var fileContent = ""
    for (filename in File(VCS_DIRECTORY + PATH_SEPARATOR + INDEX_FILE).readLines()) {
        fileContent += File(filename).readText()
    }
    return fileContent.hash()
}

fun noChanges() = File(VCS_DIRECTORY + PATH_SEPARATOR + LOG_FILE).readText().contains(getHashValue())

fun commit(commitInfo: String) {
    val logContent = "commit ${getHashValue()}\n" +
                     "Author: ${File(VCS_DIRECTORY + PATH_SEPARATOR + CONFIG_FILE).readText()}\n" +
                     "$commitInfo\n\n" +
                     File(VCS_DIRECTORY + PATH_SEPARATOR + LOG_FILE).readText()
    File(VCS_DIRECTORY + PATH_SEPARATOR + LOG_FILE).writeText(logContent)
    val newDir = VCS_DIRECTORY + PATH_SEPARATOR + COMMIT_DIRECTORY + PATH_SEPARATOR + getHashValue()
    for (filename in File(VCS_DIRECTORY + PATH_SEPARATOR + INDEX_FILE).readLines()) {
        File(filename).copyTo(File(newDir + PATH_SEPARATOR + filename))
    }
    println("Changes are committed.")
}

fun firstCommit(commitInfo: String) {
    File(VCS_DIRECTORY + PATH_SEPARATOR + LOG_FILE).writeText("")
    commit(commitInfo)
}

fun executeCommit(cmd: Commands, args: List<String>): List<String> {
    when {
        args.isEmpty() -> println("Message was not passed.")
        !File(VCS_DIRECTORY + PATH_SEPARATOR + CONFIG_FILE).exists() -> println("Please configure the user.")
        !File(VCS_DIRECTORY + PATH_SEPARATOR + INDEX_FILE).exists() -> println("No files staged.")
        !File(VCS_DIRECTORY + PATH_SEPARATOR + LOG_FILE).exists() -> firstCommit(args.first())
        noChanges() -> println("Nothing to commit.")
        else -> commit(args.first())
    }
    return emptyList()
}

fun checkout(id: String) {
    File(VCS_DIRECTORY + PATH_SEPARATOR + COMMIT_DIRECTORY + PATH_SEPARATOR + id).walk()
        .filter { item -> item.isFile }
        .forEach { file ->
        file.copyTo(File(file.toString().takeLastWhile { it != PATH_SEPARATOR }), true)
    }
    println("Switched to commit $id.")
}

fun executeCheckout(cmd: Commands, args: List<String>): List<String> {
    when {
        args.isEmpty() -> println("Commit id was not passed.")
        !File(VCS_DIRECTORY + PATH_SEPARATOR + LOG_FILE).readText().contains(args.first()) -> println("Commit does not exist.")
        else -> checkout(args.first())
    }
    return emptyList()
}

fun printHelp() {
    println("These are SVCS commands:")
    for (cmd in Commands.values()) {
        println(cmd.command.padEnd(COMMAND_SPACE) + cmd.description)
    }
}

fun execute(commandList: List<String>) {
    if (commandList.isEmpty()) return
    var noSuchCommand = true
    for (cmd in Commands.values()) {
        if (cmd.command == commandList.first()) {
            execute(cmd.action(cmd, commandList.drop(1)))
            noSuchCommand = false
        }
    }
    if (noSuchCommand) {
        println("'${commandList.first()}' is not a SVCS command.")
    }
}

fun main(args: Array<String>) {
    createVCSDirectory()
    if (args.isEmpty() || args.size == 1 && args.first() == "--help") {
        printHelp()
    }
    else {
        execute(args.toList())
    }
}