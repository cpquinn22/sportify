const { onCall, HttpsError } = require("firebase-functions/v2/https");
const functions = require("firebase-functions");
const admin = require("firebase-admin");



admin.initializeApp();

exports.sendWorkoutNotification = onCall(async ({ data }) => {
    const teamId = data.teamId;
    const workoutName = data.workoutName;
  
    console.log("✅ teamId:", teamId);
    console.log("✅ workoutName:", workoutName);
  
    if (!teamId || !workoutName) {
      console.error("❌ Missing data:", { teamId, workoutName });
      throw new HttpsError("invalid-argument", "Missing teamId or workoutName");
    }

  try {
    const teamSnap = await admin.firestore().collection("teams").doc(teamId).get();
    const members = teamSnap.get("members") || [];

    for (const memberId of members) {
      const userSnap = await admin.firestore().collection("users").doc(memberId).get();
      const token = userSnap.get("fcmToken");

      if (token) {
        const message = {
          token: token,
          notification: {
            title: "New Workout Available!",
            body: `A new workout "${workoutName}" was added to your team.`,
          },
        };

        await admin.messaging().send(message);
        console.log(`📨 Notification sent to ${memberId}`);
      } else {
        console.warn(`⚠️ No token for user ${memberId}`);
      }
    }

    return { success: true };
  } catch (error) {
    console.error("❌ Notification error:", error.message || error);
    throw new HttpsError("internal", `Notification failed: ${error.message}`);
  }
});

