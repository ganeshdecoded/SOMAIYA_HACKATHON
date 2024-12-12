package sszj.s.geminiapi.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.TextToSpeechManager
import com.example.medicinereminder.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sszj.s.geminiapi.adapter.ChatAdapter
import sszj.s.geminiapi.data.repository.ChatRepository
import sszj.s.geminiapi.utils.ChatInputEvent
import sszj.s.geminiapi.viewmodel.ChatViewModel
import sszj.s.geminiapi.viewmodel.ChatViewModelFactory
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiChatBotActivity : AppCompatActivity() {

    private val uriState = MutableStateFlow("")
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatInput: EditText
    private lateinit var addPhotoIcon: ImageView
    private lateinit var sendIcon: ImageView
    private lateinit var selectedImage: ImageView
    private lateinit var voiceInputButton: ImageButton
    private lateinit var voiceInputLauncher: ActivityResultLauncher<Intent>

    private var chatContext: String? = null
    private lateinit var ttsManager: TextToSpeechManager // TextToSpeech manager instance

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            uriState.value = uri.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.geminichatbotactivity)

        selectedImage = findViewById(R.id.selected_image)

        intent.getStringExtra("image_uri")?.let { imageUri ->
            uriState.value = imageUri
            showSelectedImage(imageUri)
        }

        chatContext = intent.getStringExtra("chat_context")

        chatViewModel = ViewModelProvider(this, ChatViewModelFactory(ChatRepository())).get(ChatViewModel::class.java)

        recyclerView = findViewById(R.id.recycler_view)
        chatInput = findViewById(R.id.chat_input)
        addPhotoIcon = findViewById(R.id.add_photo_icon)
        sendIcon = findViewById(R.id.send_icon)
        selectedImage = findViewById(R.id.selected_image)
        voiceInputButton = findViewById(R.id.voice_input_button)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = true
        recyclerView.layoutManager = layoutManager

        chatAdapter = ChatAdapter()
        recyclerView.adapter = chatAdapter

        // Initialize TextToSpeechManager
        ttsManager = TextToSpeechManager(this)

        observeViewModel()

        addPhotoIcon.setOnClickListener {
            imagePicker.launch("image/*")
        }

        sendIcon.setOnClickListener {
            val prompt = chatInput.text.toString().trim() // Get the prompt from user input

            // Check if the prompt is not empty
            if (prompt.isNotEmpty()) {
                lifecycleScope.launch {
                    val bitmap = getBitmapFromUri(uriState.value)
                    sendChatMessage(prompt, bitmap) // Send the prompt and image
                    uriState.update { "" } // Clear the image URI after sending
                    chatInput.text.clear() // Clear the input field
                }
            } else {
                Toast.makeText(this, "Please enter a prompt", Toast.LENGTH_SHORT).show()
            }
        }


        voiceInputButton.setOnClickListener {
            startVoiceInput()
        }

        voiceInputLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val matches = result.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (matches != null && matches.isNotEmpty()) {
                    chatInput.setText(matches[0])
                }
            }
        }

        // Load the image URI passed from the previous activity
        intent.getStringExtra("image_uri")?.let {
            uriState.value = it
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            chatViewModel.chatState.collect { chatState ->
                chatAdapter.submitList(chatState.chatList)
                recyclerView.scrollToPosition(chatAdapter.itemCount - 1)

                // Speak out the latest message from the bot only
                chatState.chatList.lastOrNull()?.let { lastMessage ->
                    if (!lastMessage.isUser) { // Only speak if it's a bot message
                        ttsManager.speak(lastMessage.prompt)
                    }
                }
            }
        }

        lifecycleScope.launch {
            uriState.collect { uri ->
                if (uri.isNotEmpty()) {
                    val bitmap = getBitmapFromUri(uri)
                    selectedImage.setImageBitmap(bitmap)
                    selectedImage.visibility = View.VISIBLE // Show selected image
                } else {
                    selectedImage.visibility = View.GONE // Hide when no image selected
                }
            }
        }
    }

    private suspend fun getBitmapFromUri(uri: String): Bitmap? {
        if (uri.isEmpty()) return null

        return withContext(Dispatchers.IO) {
            Glide.with(this@GeminiChatBotActivity)
                .asBitmap()
                .load(uri)
                .submit()
                .get()
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        try {
            voiceInputLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Your device does not support speech input", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun sendChatMessage(prompt: String, bitmap: Bitmap?) {
        when (chatContext) {
            "medical" -> {
                if (isMedicalQuery(prompt)) {
                    chatViewModel.onEvent(ChatInputEvent.SendPrompt(prompt, bitmap))
                } else {
                    Toast.makeText(this, "Please ask a medical-related question", Toast.LENGTH_SHORT).show()
                }
            }
            "workout" -> {
                if (isWorkoutQuery(prompt)) {
                    chatViewModel.onEvent(ChatInputEvent.SendPrompt(prompt, bitmap))
                } else {
                    Toast.makeText(this, "Please ask a workout-related question", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                chatViewModel.onEvent(ChatInputEvent.SendPrompt(prompt, bitmap))
            }
        }
    }

    // Function to determine if a query is medical-related
    private fun isMedicalQuery(prompt: String): Boolean {
        val medicalKeywords = listOf(
            "symptom", "treatment", "medicine", "doctor", "diagnosis", "prescription",
            "health", "hospital", "clinic", "specialist", "pharmacy", "surgery", "procedure",
            "check-up", "consultation", "emergency", "ambulance", "ICU", "urgent care",
            "telemedicine", "appointment", "test results", "blood test", "x-ray", "CT scan",
            "MRI", "ultrasound", "vaccine", "vaccination", "immunization", "injection", "shot",

            // Symptoms
            "fever", "pain", "headache", "migraine", "cold", "cough", "sore throat", "nausea",
            "vomiting", "fatigue", "dizziness", "diarrhea", "constipation", "shortness of breath",
            "chills", "rash", "itching", "inflammation", "swelling", "bleeding", "burn",
            "blisters", "fainting", "weakness", "anxiety", "depression", "stress", "panic attack",
            "insomnia", "weight loss", "weight gain", "high blood pressure", "low blood pressure",
            "palpitations", "chest pain", "muscle pain", "joint pain", "stiffness", "loss of appetite",
            "coughing blood", "skin discoloration", "blurred vision", "hearing loss", "earache",
            "numbness", "tingling", "seizure", "convulsion", "flu", "sneezing",

            // Diseases & Conditions
            "diabetes", "hypertension", "asthma", "allergy", "heart disease", "stroke",
            "cancer", "tumor", "arthritis", "osteoporosis", "Alzheimer's", "dementia",
            "epilepsy", "COPD", "bronchitis", "pneumonia", "COVID-19", "HIV", "AIDS",
            "hepatitis", "autoimmune disease", "anemia", "obesity", "thyroid", "hyperthyroidism",
            "hypothyroidism", "lupus", "multiple sclerosis", "kidney failure", "dialysis",
            "liver disease", "cirrhosis", "gallstones", "kidney stones", "gastroenteritis",
            "Crohn's disease", "colitis", "IBS", "ulcer", "gastritis", "acid reflux",
            "GERD", "UTI", "bladder infection", "prostate", "menopause", "endometriosis",
            "fibroids", "fertility", "infertility", "miscarriage", "pregnancy", "prenatal",
            "postnatal", "contraception", "contraceptive", "STD", "STI", "genital herpes",
            "syphilis", "gonorrhea", "chlamydia",

            // Treatments & Medications
            "antibiotic", "painkiller", "analgesic", "antidepressant", "antiviral", "anti-inflammatory",
            "steroid", "chemotherapy", "radiation", "immunotherapy", "hormone therapy",
            "antihistamine", "insulin", "blood thinner", "sedative", "anesthetic", "vaccination",
            "probiotic", "supplements", "multivitamin", "antacid", "beta blocker", "ACE inhibitor",
            "diuretic", "corticosteroid", "antiemetic", "antipsychotic", "bronchodilator",
            "inhaler", "nebulizer", "epipen", "oxygen therapy", "dialysis", "transplant",
            "physical therapy", "rehabilitation", "counseling", "therapy", "psychotherapy",
            "CBT", "occupational therapy", "speech therapy", "chiropractor", "acupuncture",
            "herbal remedy", "homeopathy", "massage therapy", "surgery", "procedure", "biopsy",
            "endoscopy", "colonoscopy", "laparoscopy", "stent", "bypass", "angioplasty",
            "pacemaker", "defibrillator",

            // Medical Specialties
            "cardiologist", "dermatologist", "endocrinologist", "gastroenterologist",
            "neurologist", "oncologist", "ophthalmologist", "orthopedic", "pediatrician",
            "psychiatrist", "psychologist", "pulmonologist", "radiologist", "surgeon",
            "urologist", "OB/GYN", "nephrologist", "rheumatologist", "immunologist",
            "infectious disease specialist", "allergist", "otolaryngologist", "ENT doctor",
            "family doctor", "general practitioner", "internal medicine", "emergency physician"

        )
        return medicalKeywords.any { keyword -> prompt.contains(keyword, ignoreCase = true) }
    }

    // Function to determine if a query is workout-related
    private fun isWorkoutQuery(prompt: String): Boolean {
        val workoutKeywords = listOf(
            "workout", "exercise", "fitness", "training", "routine", "gym", "physical activity",
            "warm-up", "cool-down", "stretch", "strength training", "cardio", "aerobic",
            "anaerobic", "flexibility", "endurance", "reps", "sets", "circuit", "high-intensity",
            "HIIT", "crossfit", "functional training", "bodyweight", "core", "balance",
            "mobility", "agility", "conditioning", "recovery", "injury prevention", "overtraining",

            // Types of Workouts
            "running", "jogging", "walking", "cycling", "swimming", "rowing", "hiking",
            "jumping rope", "jump rope", "plyometrics", "kickboxing", "martial arts", "yoga",
            "pilates", "zumba", "dance workout", "aerobics", "spinning", "sprinting", "skipping",
            "calisthenics", "gymnastics", "barre", "tai chi", "powerlifting", "weightlifting",
            "strength training", "bodybuilding", "bootcamp", "TRX", "resistance training",
            "kettlebell", "dumbbell", "barbell", "powerlifting", "olympic lifting", "isometrics",

            // Body Parts & Targeted Training
            "upper body", "lower body", "full body", "legs", "arms", "back", "shoulders", "chest",
            "glutes", "quads", "hamstrings", "calves", "biceps", "triceps", "abs", "core",
            "obliques", "lats", "deltoids", "pectorals", "trapezius", "forearms", "lower back",
            "spine", "hips", "hip flexors", "adductors", "abductors", "thoracic spine",

            // Exercises
            "push-up", "pull-up", "squat", "lunge", "deadlift", "bench press", "shoulder press",
            "overhead press", "row", "lat pulldown", "plank", "burpee", "mountain climber",
            "crunch", "sit-up", "leg raise", "bicycle crunch", "jump squat", "jump lunge",
            "box jump", "kettlebell swing", "snatch", "clean and jerk", "dead hang",
            "triceps dip", "bicep curl", "hammer curl", "glute bridge", "hip thrust",
            "calf raise", "Russian twist", "medicine ball slam", "battle ropes", "farmer's walk",

            // Cardio & Endurance
            "treadmill", "elliptical", "stationary bike", "rowing machine", "jumping jacks",
            "high knees", "butt kicks", "box step-ups", "hill sprints", "intervals", "tempo run",
            "distance run", "steady state cardio", "stair climber", "cardio circuit",
            "aerobic endurance", "VO2 max", "heart rate", "target heart rate",

            // Fitness Goals
            "weight loss", "fat loss", "muscle gain", "bulking", "cutting", "toning",
            "lean muscle", "strength gain", "power", "hypertrophy", "body fat percentage",
            "BMI", "calorie burn", "metabolism", "caloric deficit", "caloric surplus",
            "body composition", "performance", "endurance", "speed", "stamina", "agility",
            "flexibility", "balance", "coordination", "explosiveness", "mobility", "rehabilitation",

            // Equipment
            "dumbbell", "barbell", "kettlebell", "resistance band", "pull-up bar", "medicine ball",
            "stability ball", "BOSU ball", "foam roller", "TRX straps", "rowing machine",
            "treadmill", "elliptical", "exercise bike", "bench", "weight plates", "squat rack",
            "power rack", "smith machine", "leg press", "cable machine", "lat pulldown machine",
            "gym mat", "jump rope", "battle ropes", "yoga mat",

            // Fitness Programs & Styles
            "HIIT", "circuit training", "strength training", "powerlifting", "bodybuilding",
            "crossfit", "functional fitness", "endurance training", "marathon training",
            "triathlon training", "5K", "10K", "half marathon", "ultramarathon", "bootcamp",
            "group fitness", "personal training", "home workout", "gym workout", "fitness class",
            "online workout", "virtual training", "fitness app", "fitness tracker", "wearable",

            // Nutrition & Supplements
            "protein", "whey protein", "creatine", "BCAA", "pre-workout", "post-workout",
            "hydration", "electrolytes", "meal plan", "calories", "macros", "carbs", "fat",
            "fiber", "protein powder", "smoothies", "recovery drink", "meal prep",
            "energy drink", "sports drink", "nutrition", "diet", "keto", "paleo",
            "plant-based", "vegan", "vegetarian", "carb cycling", "intermittent fasting",

            // Recovery & Injury Prevention
            "rest day", "active recovery", "stretching", "foam rolling", "dynamic stretching",
            "static stretching", "mobility work", "yoga for recovery", "sleep", "hydration",
            "massage", "ice bath", "compression", "taping", "physical therapy", "sports therapy",
            "injury prevention", "sprain", "strain", "tendonitis", "muscle soreness", "DOMS",
            "joint pain", "rehab", "physiotherapy", "ACL", "rotator cuff", "shin splints",
            "stress fracture", "runner's knee", "tennis elbow", "golfer's elbow"

        )
        return workoutKeywords.any { keyword -> prompt.contains(keyword, ignoreCase = true) }
    }

    private fun showSelectedImage(imageUri: String) {
        Glide.with(this)
            .load(imageUri)
            .into(selectedImage)
        selectedImage.visibility = View.VISIBLE // Make sure the image view is visible
    }
}

