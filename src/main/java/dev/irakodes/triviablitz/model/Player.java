package dev.irakodes.triviablitz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {

    private String id;
    private String name;
    private boolean host;
    private int score;
}
