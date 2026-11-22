package ma.ensam.mycampus.dao.entities.enums;

public enum StatutSignalement {
    EN_ATTENTE,   // signalé par un étudiant, pas encore traité par un gestionnaire
    VALIDEE,      // panne confirmée : le matériel est en panne
    REFUSEE       // le gestionnaire n'a pas confirmé la panne
}