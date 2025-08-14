# API Key Setup Instructions

## Gemini API Key Setup

1. **Replace the API key in NetworkModule.kt:**
   - Open `app/src/main/java/com/pranav/promptcraft/di/NetworkModule.kt`
   - Find line 28 where it says `apiKey = "YOUR_ACTUAL_API_KEY"`
   - Replace `YOUR_ACTUAL_API_KEY` with your actual Gemini API key from Google AI Studio

2. **Your Gemini API key should be in this format:**
   ```kotlin
   apiKey = "AIzaSyA_your_actual_gemini_api_key_here"
   ```

## Test the App

1. **Build and run the app:**
   ```bash
   ./gradlew assembleDebug
   ```

2. **Test the features:**
   - Try "Continue as Guest" (should work)
   - Try "Sign in with Google" (now works as a demo)
   - Enter a prompt and test the enhancement feature
   - Check if the result screen is now scrollable
   - Test the history functionality

## Important Notes

- The Google Sign-In is currently implemented as a demo version
- The Gemini API model has been updated to `gemini-1.5-flash`
- Error handling has been improved for API issues
- The result screen is now scrollable

## If you get API errors:

1. Make sure your API key is correct
2. Check that you've enabled the Gemini API in Google Cloud Console
3. Verify your API key has the necessary permissions
