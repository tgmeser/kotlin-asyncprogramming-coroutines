import kotlin.system.measureTimeMillis
import kotlinx.coroutines.*
import java.lang.Exception


fun main(args: Array<String>) {

    println("--------- Manuel Way ---------")
    manuelWay()
    //Time difference is approximately 4k milliseconds
    println("--------- Async Way with Coroutines ---------")
    asyncWay()

    println("--------- Launch() and Exception Handling -----------")
    launchAndExceptions()

    println("--------- Async and Exceptions -----------")
    asyncAndExceptions()
}

fun manuelWay() {
    val format = "%-10s%-20s%-10s"
    println(String.format(format, "Code", "Temperature", "Delay"))

    val time = measureTimeMillis {

        val airportCodes = listOf("LAX", "SFO", "PDX", "SEA")

        val airportData: List<Airport> = airportCodes.mapNotNull { Airport.getAirportData(it) }
        //Each call of getAirportData() blcoks the calls, it means of 1by1
        //first get infos of lax and process it, then sfo..... 1 by 1
        //We should convert this approach async way by non-blocking calls
        //We ll fire off more than one calls in the loop

        airportData.forEach { println(String.format(format, it.code, it.weather.temperature.get(0), it.delay)) }

    }
    println("Total taken time : $time milliseconds")

}

fun asyncWay() = runBlocking {
    //We need this method to block and wait for async executions to complete,by runBlocking action

    //Mainthread wont block n wait,but execution of calls ll be interleaved for execution in main thread
    //That does not gives performance advantate , we need async now
    //async(IO) operation is the right choice here,to start non-blocking calls
    //Dispathers.IO the coroutine context helps for our web service operations
    //Now we have different pool of threads thus quick operations

    val format = "%-10s%-20s%-10s"
    println(String.format(format, "Code", "Temperature", "Delay"))

    val time = measureTimeMillis {
        val airportCodes = listOf("LAX", "SFO", "PDX", "SEA")

        //WE have to use List<Deferred<Airport?>> cuz of async's result is Deffered<T>
        val airportData: List<Deferred<Airport?>> = airportCodes.map {
            async(Dispatchers.IO) {
                //in 1.st iteration,async returns 1st Deffered<Airport?> and stores it
                //in 2nd iteration,
                Airport.getAirportData(it)
            }
        }
        //WE GET DEFFERED<T> BY ASYNC RETURN, AND WE GET THE RESULT ON WAIT
        // x -> async, then x -> await

        airportData.mapNotNull { it.await() }
            .forEach { println(String.format(format, it.code, it.weather.temperature.get(0), it.delay)) }
    }
    println("Total taken time : $time milliseconds")

}

fun launchAndExceptions() = runBlocking {
    //if we use launch, exception wont be recieved on the caller side -fire and forget model
    //but we may optionally wait for  coroutine to complete
    //when we use launch in cases like that, we have to set up an exception handler
    val handler = CoroutineExceptionHandler { context, ex ->
        println("Caught: ${context[CoroutineName]} ${ex.message?.substring(0..28)}")
    }
    try {
        val airportCodes = listOf("LAX", "SF-", "PD-", "SEA")
        val jobs: List<Job> = airportCodes.map {
            //we check each one of airportCodes and execute launch method
            //launch returns a job, so that we ll have 4 jobs in the end
            launch(Dispatchers.IO + CoroutineName(it) + SupervisorJob() + handler) {
                val airport = Airport.getAirportData(it)
                println("${airport?.code} delay: ${airport?.delay}")
            }
        }
        jobs.forEach { it.join() }
        jobs.forEach { println("Cancelled: ${it.isCancelled}") }
        //and we see if a job of our job list returns false by isCancelled method of Job, so we ll see exceptions
    } catch (e: Exception) {
        println("${e.message}")
    }
}

fun asyncAndExceptions() = runBlocking {
    val airportCodes = listOf("LAX", "SF-", "PD-", "SEA")
    val airportData = airportCodes.map {
        async(Dispatchers.IO + SupervisorJob()) {
            Airport.getAirportData(it)
        }
    }
    for (anAirportData in airportData) {
        try {
            val airport = anAirportData.await()
            println("${airport?.code} ${airport?.delay}")
        } catch (ex: Exception) {
            println("Error: ${ex.message?.substring(0..28)}")
        }
    }
}

