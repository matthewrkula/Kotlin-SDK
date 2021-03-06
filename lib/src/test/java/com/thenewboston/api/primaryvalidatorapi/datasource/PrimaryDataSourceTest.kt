package com.thenewboston.api.primaryvalidatorapi.datasource

import com.thenewboston.api.common.GetDataSource
import com.thenewboston.api.common.PostDataSource
import com.thenewboston.common.http.Outcome
import com.thenewboston.utils.Mocks
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.contain
import io.kotest.matchers.types.beInstanceOf
import io.ktor.util.*
import io.ktor.utils.io.errors.*
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle

@KtorExperimentalAPI
@ExperimentalCoroutinesApi
@TestInstance(Lifecycle.PER_CLASS)
class PrimaryDataSourceTest {

    @MockK
    lateinit var getDataSource: GetDataSource

    @MockK
    lateinit var postDataSource: PostDataSource

    private lateinit var primaryDataSource: PrimaryDataSource

    @BeforeAll
    fun setup() {
        MockKAnnotations.init(this)

        primaryDataSource = PrimaryDataSource(getDataSource, postDataSource)
    }

    @Nested
    @DisplayName("Given successful request...")
    @TestInstance(Lifecycle.PER_CLASS)
    inner class GivenSucceedingRequest {

        @Nested
        @DisplayName("When performing a GET request...")
        @TestInstance(Lifecycle.PER_CLASS)
        inner class WhenGetRequest {

            private val paginationTwenty = Mocks.paginationOptionsTwenty()
            private val paginationThirty = Mocks.paginationOptionsThirty()

            @Test
            fun `should fetch primary validator details from config`() = runBlockingTest {
                coEvery { getDataSource.primaryValidatorDetails() } returns Outcome.Success(Mocks.primaryValidatorDetails())

                val response = primaryDataSource.fetchPrimaryValidatorDetails()

                check(response is Outcome.Success)
                response.value.nodeType shouldBe "PRIMARY_VALIDATOR"
                response.value.rootAccountFile shouldBe "http://20.188.33.93/media/root_account_file.json"
                response.value.ipAddress should contain("172.19.0.13")
            }

            @Test
            fun `should fetch list of 20 validators successfully`() = runBlockingTest {
                val value = Mocks.validators(paginationTwenty)

                coEvery { getDataSource.validators(paginationTwenty) } returns Outcome.Success(value)
                val response = primaryDataSource.fetchValidators(paginationTwenty)

                check(response is Outcome.Success)
                response.value.results.shouldNotBeEmpty()
                response.value.count shouldBeGreaterThan 20
                response.value.results.size shouldBeLessThanOrEqual 20
            }

            @Test
            fun `should fetch list of 30 validators successfully`() = runBlockingTest {
                val value = Mocks.validators(paginationThirty)
                coEvery { getDataSource.validators(paginationThirty) } returns Outcome.Success(value)

                val response = primaryDataSource.fetchValidators(paginationThirty)

                check(response is Outcome.Success)
                response.value.results.shouldNotBeEmpty()
                response.value.count shouldBeGreaterThan 0
                response.value.results.size shouldBeLessThanOrEqual 30
            }

            @Test
            fun `should fetch single validator successfully`() = runBlockingTest {
                val nodeIdentifier =
                    "6871913581c3e689c9f39853a77e7263a96fd38596e9139f40a367e28364da53"

                coEvery { getDataSource.validator(nodeIdentifier) } returns Outcome.Success(Mocks.validator())

                val response = primaryDataSource.fetchValidator(nodeIdentifier)

                check(response is Outcome.Success)
                response.value.nodeIdentifier should contain(nodeIdentifier)
                response.value.ipAddress should contain("127.0.0.1")
            }
        }
    }

    @Nested
    @DisplayName("Given request that should fail")
    @TestInstance(Lifecycle.PER_CLASS)
    inner class GivenFailingRequest {

        @Nested
        @DisplayName("When performing GET request...")
        @TestInstance(Lifecycle.PER_CLASS)
        inner class WhenGetRequest {

            private val pagination = Mocks.paginationOptionsDefault()

            @Test
            fun `should return error outcome for primary validator details IOException`() = runBlockingTest {

                val message = "Failed to retrieve primary validator details"
                coEvery { getDataSource.primaryValidatorDetails() } returns Outcome.Error(message, IOException())

                val response = primaryDataSource.fetchPrimaryValidatorDetails()

                check(response is Outcome.Error)
                response.cause should beInstanceOf<IOException>()
                response.message shouldBe message
            }

            @Test
            fun `should return error outcome for list of validators IOException`() = runBlockingTest {
                val message = "Could not fetch list of accounts"
                coEvery { getDataSource.validators(pagination) } returns Outcome.Error(message, IOException())

                val response = primaryDataSource.fetchValidators(pagination)

                check(response is Outcome.Error)
                response.cause should beInstanceOf<IOException>()
                response.message shouldBe message
            }

            @Test
            fun `should return error outcome for single validator`() = runBlockingTest {
                val nodeIdentifier = "6871913581c3e689c9f39853a77e7263a96fd38596e9139f40a367e28364da53"
                val message = "Could not fetch validator with NID $nodeIdentifier"

                coEvery { getDataSource.validator(nodeIdentifier) } returns Outcome.Error(message, IOException())

                val response = primaryDataSource.fetchValidator(nodeIdentifier)

                check(response is Outcome.Error)
                response.cause should beInstanceOf<IOException>()
                response.message shouldBe message
            }
        }
    }
}
