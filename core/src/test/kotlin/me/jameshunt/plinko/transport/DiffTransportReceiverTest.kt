package me.jameshunt.plinko.transport

import me.jameshunt.plinko.merkle.DiffGenerator
import me.jameshunt.plinko.merkle.toHashObject
import me.jameshunt.plinko.store.Plinko
import me.jameshunt.plinko.store.domain.Commit
import me.jameshunt.plinko.toJson
import me.jameshunt.plinko.tree1
import me.jameshunt.plinko.tree9
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.time.OffsetDateTime

class DiffTransportReceiverTest {

    @Test
    fun test() {
        val document = Plinko.collection("testFormat").document("testReceiver")
        document.setData(
            File("src/test/resources/9.json")
                .readText()
                .toJson()
        )

        val diff = DiffGenerator.getDiff(tree1.toHashObject(), tree9.toHashObject())
        val commit = Commit(1, OffsetDateTime.now(), diff)
        val transport = DiffTransportFormatter.format(commit)

        val commitValues = DiffTransportReceiver.asCommitAndValues(transport)
        println(Plinko.objectMapper.writeValueAsString(commitValues))

        val expectedHashToValue = mapOf(
            "09f33e9079ba8f01ba1c219fb294e676" to "arrayChange",
            "13b73edae8443990be1aa8f1a483bc27" to "dude",
            "2063c1608d6e0baf80249c42e2be5804" to "value",
            "2eeecd72c567401e6988624b179d0b14" to "sup",
            "3c6e0b8a9c15224a8228b9a98ca1531d" to "key",
            "4bdb23fce4d3c6b21f8b8e3c913f7cf9" to "addKeyToObjectTest",
            "4cdf5a25d4673bfc4546ca7843071f65" to "dis",
            "5448223ad8d7a8fca661b406c750bde7" to "be cool",
            "74e8333ad11685ff3bdae589c8f6e34d" to "down",
            "85b8a5f948a4195f482ab1c477ce4cfd" to "myArray",
            "9fb81d8f33d6f85c11c81aa6a45b40ba" to "adultObject",
            "b326b5062b2f0e69046810717534cb09" to true,
            "bcedc450f8481e89b1445069acdc3dd9" to "wow",
            "d749904cbebce14f5612e4e600c48174" to "foogliest"
        )

        Assert.assertEquals(expectedHashToValue, commitValues.newValues)
    }
}
