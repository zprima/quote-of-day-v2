package com.example.quoteofdayv2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quoteofdayv2.ui.QuoteOfDayV2Theme
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.chrisbanes.accompanist.coil.CoilImage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

// Helpful logging to see http body content
internal val baseOkHttpClient: OkHttpClient = OkHttpClient
    .Builder()
    .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
    .build()

// Retrofit interface for REST api service
interface QuoteOfDayService{
    @GET("qod?language=en")
    suspend fun quoteOfDay() : Response<QuoteOfDayJsonResponse?>
}

// Representation of quote of day response
class QuoteOfDayJsonResponse(
    val contents: Map<String, List<QuoteOfDay>>
)

// Representation of quote
data class QuoteOfDay(
    val quote: String,
    val author: String,
    val background: String
)

// Representation of error response
class ErrorResponse(
    val error: ErrorBodyJsonResponse
)

// Representation of error response content
class ErrorBodyJsonResponse(
    val code: Int,
    val message: String
)

// Ambient for the QuoteOfDayService to be accessible by any Composable down the tree
val AmbientQuoteOfDay = ambientOf<QuoteOfDayService?> { null }


class MainActivity : AppCompatActivity() {
    // General Mochi json serializer
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Retrofit build instance that will connect to the quotes REST api
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://quotes.rest/")
        .client(baseOkHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    // Instance of the retrofit which connects to an interface
    private val api = retrofit.create(QuoteOfDayService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // little theming
            QuoteOfDayV2Theme(darkTheme = true) {
                // defining ambient providers
                Providers(AmbientQuoteOfDay provides api){
                    Surface(
                        color = MaterialTheme.colors.background,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ){
                            QuoteCardContainer()
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun QuoteCardContainer(){
    // getting the current ambient provider for our quote of day service api
    val api = AmbientQuoteOfDay.current

    var quoteOfDay by remember { mutableStateOf<QuoteOfDay?>(null) }
    var errorMessage by remember{ mutableStateOf<String>("")}

    // a coroutine that will call the api to get the quote
    // if an error occurs it will handle it
    LaunchedTask(){
        if(api != null && quoteOfDay == null) {
            val response = api.quoteOfDay()
            if(response.isSuccessful){
                quoteOfDay = response.body()?.contents?.get("quotes")?.get(0)
            } else {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val jsonAdapter: JsonAdapter<ErrorResponse> =
                    moshi.adapter<ErrorResponse>(ErrorResponse::class.java)

                val string = response.errorBody()?.string()!!
                val errResponse: ErrorResponse = jsonAdapter.fromJson(string)!!
                errorMessage = errResponse.error.message
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(all = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color = Color.DarkGray),

        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        if(quoteOfDay != null) QuoteCard(quoteOfDay!!)
        else QuoteCardEmpty(errorMessage)
    }
}

@Composable
fun QuoteCardEmpty(errorMessage: String){
    if(errorMessage.isEmpty()) Text("Loading")
    else Text(errorMessage, modifier = Modifier.padding(10.dp))
}

@Composable
fun QuoteCard(quoteOfDay: QuoteOfDay){
    CoilImage(
        data = quoteOfDay.background,
        loading = {
            Box(modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
    Column(
        modifier = Modifier.padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            quoteOfDay.quote,
            fontSize = 21.sp,
            textAlign = TextAlign.Center
        )
        Spacer(
            modifier = Modifier.preferredHeight(16.dp)
        )
        Text(
            quoteOfDay.author
        )
    }
}


