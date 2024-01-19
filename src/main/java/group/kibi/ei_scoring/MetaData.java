package group.kibi.ei_scoring;

public class MetaData {
    private String participantId;
    private String itemNumber;

    public MetaData(String fileName) {
        //remove file type in file name
        fileName = fileName.replaceAll(".raw", "");
        fileName = fileName.replaceAll(".mp3", "");

        String fileNameParts[] = fileName.split("_");
        participantId = fileNameParts[0];
//        itemNumber = fileNameParts[1].split("-")[1];
        itemNumber = fileName;
    }

    public String getParticipantId() {
        return participantId;
    }

    public String getItemNumber() {
        return itemNumber;
    }

}
