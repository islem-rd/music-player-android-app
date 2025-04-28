package com.example.musicplayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to convert between itempl and Liste objects
 */
public class ItemplToListeConverter {

    /**
     * Convert a list of itempl objects to Liste objects
     */
    public static List<Liste> convertToListe(List<itempl> itemplList) {
        List<Liste> listeList = new ArrayList<>();

        for (itempl item : itemplList) {
            Liste liste = new Liste(
                    item.getTitle(),
                    item.getArtist(),
                    item.getCoverImg()
            );
            listeList.add(liste);
        }

        return listeList;
    }

    /**
     * Convert a list of Liste objects to itempl objects
     */
    public static List<itempl> convertToItempl(List<Liste> listeList) {
        List<itempl> itemplList = new ArrayList<>();

        for (Liste liste : listeList) {
            itempl item = new itempl(
                    liste.getDescriptplaylist(),
                    liste.getPlaylisttitle(),
                    liste.getCoverResourceId()
            );
            itemplList.add(item);
        }

        return itemplList;
    }
}
