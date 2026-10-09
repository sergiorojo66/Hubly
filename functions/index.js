const functions = require("firebase-functions/v2/https");
const { HttpsError, onCall } = functions;
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

const MAX_TITLE_LENGTH = 100;
const MAX_BODY_LENGTH = 500;

exports.sendEventNotification = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Debes iniciar sesión para enviar notificaciones.");
  }
  const { eventId, title, body } = request.data || {};
  if (typeof eventId !== "string" || !eventId.trim()) {
    throw new HttpsError("invalid-argument", "Falta el ID del evento.");
  }
  if (typeof title !== "string" || !title.trim() || typeof body !== "string" || !body.trim()) {
    throw new HttpsError("invalid-argument", "Falta título o cuerpo de la notificación.");
  }
  const eventSnap = await db.collection("events").doc(eventId).get();
  if (!eventSnap.exists) {
    throw new HttpsError("not-found", "Evento no encontrado.");
  }
  const event = eventSnap.data();
  const callerUid = request.auth.uid;
  const participantIds = Array.isArray(event.participantsIds) ? event.participantsIds : [];
  const isParticipant = participantIds.includes(callerUid) || event.organizer === callerUid;
  if (!isParticipant) {
    throw new HttpsError("permission-denied", "No formas parte de este evento.");
  }
  const recipientIds = participantIds.filter((id) => id !== callerUid);
  if (recipientIds.length === 0) return { sent: 0, failed: 0 };
  const userDocs = await db.getAll(...recipientIds.map((id) => db.collection("users").doc(id)));
  const tokens = userDocs
    .map((doc) => doc.get("fcmToken"))
    .filter((token) => typeof token === "string" && token.trim().length > 0);
  if (tokens.length === 0) return { sent: 0, failed: 0 };
  const message = {
    notification: { title: title.slice(0, MAX_TITLE_LENGTH), body: body.slice(0, MAX_BODY_LENGTH) },
    tokens,
  };
  const response = await admin.messaging().sendEachForMulticast(message);
  return { sent: response.successCount, failed: response.failureCount };
});

exports.joinPrivateEvent = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Debes iniciar sesión para unirte a un evento.");
  }
  const { eventId, password } = request.data || {};
  if (typeof eventId !== "string" || !eventId.trim()) {
    throw new HttpsError("invalid-argument", "Falta el ID del evento.");
  }
  const callerUid = request.auth.uid;
  const eventRef = db.collection("events").doc(eventId);
  // La contraseña real vive aparte del documento público del evento (ver
  // EventRepositoryImpl.kt en el cliente); solo este backend puede leerla,
  // gracias al Admin SDK, que no está sujeto a las reglas de Firestore.
  const privateConfigRef = eventRef.collection("private").doc("config");

  return db.runTransaction(async (tx) => {
    // Firestore exige hacer todas las lecturas antes que cualquier escritura
    // dentro de una misma transacción.
    const snap = await tx.get(eventRef);
    if (!snap.exists) {
      throw new HttpsError("not-found", "Evento no encontrado.");
    }
    const event = snap.data();
    const participantIds = Array.isArray(event.participantsIds) ? event.participantsIds : [];

    if (participantIds.includes(callerUid)) {
      return { joined: true, alreadyMember: true };
    }

    const maxParticipants = event.maxParticipants;
    if (typeof maxParticipants === "number" && participantIds.length >= maxParticipants) {
      throw new HttpsError("resource-exhausted", "El evento está completo.");
    }

    if (event.isPrivate) {
      const privateSnap = await tx.get(privateConfigRef);
      const storedPassword = privateSnap.exists && typeof privateSnap.data().password === "string"
        ? privateSnap.data().password
        : "";
      if (!password || password !== storedPassword) {
        throw new HttpsError("permission-denied", "Contraseña incorrecta.");
      }
    }

    tx.update(eventRef, { participantsIds: admin.firestore.FieldValue.arrayUnion(callerUid) });
    return { joined: true, alreadyMember: false };
  });
});
