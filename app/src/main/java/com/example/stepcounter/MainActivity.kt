package com.example.stepcounter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.stepcounter.databinding.ActivityMainBinding
import kotlin.math.roundToInt
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import android.util.Log

class MainActivity : AppCompatActivity() , View.OnClickListener , SensorEventListener {

    lateinit var addBtn : Button
    lateinit var subBtn : Button
    lateinit var multBtn : Button
    lateinit var divBtn : Button
    lateinit var et1 : EditText
    lateinit var et2 : EditText
    lateinit var resultTv : TextView

    lateinit var timerText : TextView
    lateinit var startBtn : Button
    lateinit var resetBtn : Button

    lateinit var binding : ActivityMainBinding
    var timerStarted : Boolean = false
    lateinit var serviceIntent: Intent
    var time = 0.0

    private var sensorManager : SensorManager? = null
    private var running = false
    private var totalSteps = 0f
    private var prevTotalSteps = 0f

    lateinit var distServiceIntent: Intent
    var distanceTravelled = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addBtn.setOnClickListener(this) // can i change the argument here so that calculator buttons have a named function not overriding the common OnClick for this activity
        binding.subBtn.setOnClickListener(this)
        binding.multBtn.setOnClickListener(this)
        binding.divBtn.setOnClickListener(this)

        binding.startBtn.setOnClickListener { startStopTimer() }
        binding.resetBtn.setOnClickListener { resetTimer() }

        serviceIntent = Intent(applicationContext, TimerService::class.java)
        registerReceiver(updateTime, IntentFilter(TimerService.TIMER_UPDATED), RECEIVER_EXPORTED)

        loadData()
        resetSteps()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        distServiceIntent = Intent(applicationContext, DistanceTravelledService::class.java)
        registerReceiver(updateDistance, IntentFilter(DistanceTravelledService.DISTANCE_UPDATED), RECEIVER_EXPORTED)
        resetDistance()
        startService(distServiceIntent)
    }

    /*
        DISTANCE TRAVELLED
     */
    private val updateDistance: BroadcastReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            distanceTravelled = intent.getDoubleExtra(DistanceTravelledService.DISTANCE_EXTRA, 0.0)
            binding.distanceText.text = String.format("%.0f %s", distanceTravelled, "m")
        }
    }

    private fun resetDistance() {
        var distanceTv = binding.distanceText
        distanceTv.setOnClickListener {
            Toast.makeText(this, "Hold to reset distance travelled", Toast.LENGTH_SHORT).show()
        }

        distanceTv.setOnLongClickListener {

            distServiceIntent.putExtra(DistanceTravelledService.DISTANCE_EXTRA, 0.0)
            distanceTv.text = String.format("%.0f %s", 0.0, "m")

            // Presumably this is the return of the callback?
            true
        }
    }

    /*
        PEDOMETER
     */
    override fun onResume() {
        super.onResume()
        running = true

        // Returns the number of steps taken by the user since the last reboot while activated
        // This sensor requires permission android.permission.ACTIVITY_RECOGNITION.
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)


        if (stepSensor == null) {
            Toast.makeText(this, "No sensor detected on this device", Toast.LENGTH_SHORT).show()
        } else {
            // Rate suitable for the user interface
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        var stepsTakenTv = binding.stepText

        if (running) {
            totalSteps = event!!.values[0]

            val currentSteps = totalSteps.toInt() - prevTotalSteps.toInt()

            stepsTakenTv.text = ("$currentSteps steps")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }

    private fun resetSteps() {
        var stepsTakenTv = binding.stepText
        stepsTakenTv.setOnClickListener {
            Toast.makeText(this, "Hold to reset steps", Toast.LENGTH_SHORT).show()
        }

        stepsTakenTv.setOnLongClickListener {

            prevTotalSteps = totalSteps

            // When the user will click long tap on the screen,
            // the steps will be reset to 0
            stepsTakenTv.text = "0 steps"

            saveData()

            // Presumably this is the return of the callback?
            true
        }
    }

    private fun saveData() {
        // Shared Preferences will allow us to save
        // and retrieve data in the form of key,value pair.
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

        val editor = sharedPreferences.edit()
        editor.putFloat("key1", prevTotalSteps)
        editor.apply()
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val savedSteps = sharedPreferences.getFloat("key1", 0f)

        // Log.d is used for debugging purposes
        Log.d("MainActivity", "$savedSteps")

        prevTotalSteps = savedSteps
    }

    /*
        TIMER
     */
    private fun resetTimer()
    {
        stopTimer()
        time = 0.0
        binding.timerText.text = getTimeStringFromDouble(time)
    }

    private fun startStopTimer()
    {
        if(timerStarted)
            stopTimer()
        else
            startTimer()
    }

    private fun startTimer()
    {
        serviceIntent.putExtra(TimerService.TIME_EXTRA, time)
        startService(serviceIntent)
        binding.startBtn.text = "Stop"
        binding.startBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_pause_24,0,0,0)
        timerStarted = true
    }

    private fun stopTimer()
    {
        stopService(serviceIntent)
        binding.startBtn.text = "Start"
        binding.startBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_play_arrow_24,0,0,0)
        timerStarted = false
    }

    private val updateTime: BroadcastReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            time = intent.getDoubleExtra(TimerService.TIME_EXTRA, 0.0)
            binding.timerText.text = getTimeStringFromDouble(time)
        }
    }

    private fun getTimeStringFromDouble(time: Double): String
    {
        val resultInt = time.roundToInt()
        val hours = resultInt % 86400 / 3600
        val minutes = resultInt % 86400 % 3600 / 60
        val seconds = resultInt % 86400 % 3600 % 60

        return makeTimeString(hours, minutes, seconds)
    }

    private fun makeTimeString(hour: Int, min: Int, sec: Int): String = String.format("%02d:%02d:%02d", hour, min, sec)

    fun startStopTapped(v :View?) {
        timerStarted = !timerStarted
        binding.startBtn.text = if (timerStarted) "stop" else "start"

        if (timerStarted) {
            binding.startBtn.setTextColor(ContextCompat.getColor(applicationContext, R.color.red))

            startTimer()
        }
        else {
            binding.startBtn.setTextColor(ContextCompat.getColor(applicationContext, R.color.green))
        }
    }

    /*
        CALCULATOR
     */
    override fun onClick(v: View?) {
        var a = binding.et1.text.toString().toDouble()
        var b = binding.et2.text.toString().toDouble()
        var res = 0.0
        when(v?.id){
            R.id.addBtn ->{
                res = a+b
            }
            R.id.subBtn ->{
                res = a-b
            }
            R.id.multBtn ->{
                res = a*b
            }
            R.id.divBtn ->{
                res = a/b
            }
        }
        binding.resultTv.text = "The answer is $res :D"
    }
}