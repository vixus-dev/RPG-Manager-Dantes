package br.com.dantesrpg.model.enums;

public enum ModoAtaque {
	NORMAL, // Padrão
	FRACO, // Melee: 0.75x Dano, -20% TU
	FORTE, // Melee/Ranged: 1.25x Dano, +20% TU, +1 Alcance
	CORONHADA // Ranged: 0.5x Dano, Melee, TU Padrão
}