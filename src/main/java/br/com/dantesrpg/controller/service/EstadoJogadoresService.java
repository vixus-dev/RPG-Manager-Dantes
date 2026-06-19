package br.com.dantesrpg.controller.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.util.PartyPreset;
import br.com.dantesrpg.model.util.PartyPresetsData;

public class EstadoJogadoresService {

	private final Supplier<EstadoCombate> estadoSupplier;
	private final Function<String, Personagem> carregarPersonagem;
	private final Consumer<Personagem> salvarPersonagem;
	private final Predicate<Personagem> isPlayer;
	private final Runnable atualizarInterfaceTotal;

	public EstadoJogadoresService(Supplier<EstadoCombate> estadoSupplier,
			Function<String, Personagem> carregarPersonagem, Consumer<Personagem> salvarPersonagem,
			Predicate<Personagem> isPlayer, Runnable atualizarInterfaceTotal) {
		this.estadoSupplier = estadoSupplier;
		this.carregarPersonagem = carregarPersonagem;
		this.salvarPersonagem = salvarPersonagem;
		this.isPlayer = isPlayer;
		this.atualizarInterfaceTotal = atualizarInterfaceTotal;
	}

	public List<Personagem> criarJogadoresIniciais() {
		List<Personagem> players = new ArrayList<>();

		// Tenta carregar o preset equipado
		PartyPresetsData presetsData = PartyPresetsData.carregarPresets();
		if (presetsData != null && presetsData.getEquippedSlotName() != null) {
			String equippedName = presetsData.getEquippedSlotName();
			PartyPreset equippedPreset = presetsData.getPresets().stream()
					.filter(p -> p.getName().equalsIgnoreCase(equippedName))
					.findFirst().orElse(null);
			if (equippedPreset != null && !equippedPreset.getCharacterNames().isEmpty()) {
				int count = 0;
				for (String name : equippedPreset.getCharacterNames()) {
					Personagem p = carregarPersonagem.apply(name);
					if (p != null) {
						p.setPosX(4 + (count % 3));
						p.setPosY(4 + (count / 3));
						players.add(p);
						count++;
					}
				}
				if (!players.isEmpty()) {
					System.out.println("SISTEMA: Inicializado com a equipe equipada '" + equippedName + "' (" + players.size() + " personagens).");
					return players;
				}
			}
		}

		// Fallback: Jogadores padrão
		System.out.println("SISTEMA: Nenhum preset equipado encontrado. Inicializando com jogadores padrão.");
		Personagem alexei = carregarPersonagem.apply("alexei");
		Personagem lilith = carregarPersonagem.apply("lilith");
		Personagem eidan = carregarPersonagem.apply("eidan");
		Personagem arkos = carregarPersonagem.apply("Arkos");
		Personagem kuangLi = carregarPersonagem.apply("KuangLi");
		Personagem pinocchio = carregarPersonagem.apply("Pinocchio");

		if (eidan != null) {
			eidan.setPosX(4);
			eidan.setPosY(4);
			players.add(eidan);
		}
		if (alexei != null) {
			alexei.setPosX(5);
			alexei.setPosY(4);
			players.add(alexei);
		}
		if (lilith != null) {
			lilith.setPosX(4);
			lilith.setPosY(5);
			players.add(lilith);
		}
		if (kuangLi != null) {
			kuangLi.setPosX(5);
			kuangLi.setPosY(5);
			players.add(kuangLi);
		}
		if (pinocchio != null) {
			pinocchio.setPosX(3);
			pinocchio.setPosY(4);
			players.add(pinocchio);
		}
		return players;
	}

	public List<String> listarArquivosPersonagens() {
		List<String> nomes = new ArrayList<>();
		String projectPath = System.getProperty("user.dir");
		String[] caminhos = {
				"/src/main/resources/data/players/",
				"/resources/data/players/",
				"/src/data/players/"
		};

		Set<String> setNomes = new HashSet<>();
		for (String caminho : caminhos) {
			File dir = new File(projectPath + caminho);
			if (dir.exists() && dir.isDirectory()) {
				File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
				if (files != null) {
					for (File file : files) {
						setNomes.add(file.getName().replace(".json", ""));
					}
				}
			}
		}
		nomes.addAll(setNomes);
		nomes.sort(String.CASE_INSENSITIVE_ORDER);
		return nomes;
	}

	public void carregarEstadoJogadores() {
		System.out.println("SISTEMA: Recarregando estado dos jogadores...");
		EstadoCombate estado = estadoSupplier.get();
		List<Personagem> combatentesAtuais = estado.getCombatentes();

		for (int i = 0; i < combatentesAtuais.size(); i++) {
			Personagem personagem = combatentesAtuais.get(i);
			if (isPlayer.test(personagem) && personagem.getJsonFileName() != null) {
				String nomeArquivo = personagem.getJsonFileName().replace(".json", "");
				Personagem originalDoDisco = carregarPersonagem.apply(nomeArquivo);
				if (originalDoDisco != null) {
					originalDoDisco.setPosX(personagem.getPosX());
					originalDoDisco.setPosY(personagem.getPosY());
					originalDoDisco.setContadorTU(personagem.getContadorTU());
					originalDoDisco.setAusente(personagem.isAusente());
					originalDoDisco.setProtagonista(personagem.isProtagonista());
					originalDoDisco.setMovimentoRestanteTurno(personagem.getMovimentoRestanteTurno());

					combatentesAtuais.set(i, originalDoDisco);
					System.out.println("Recarregado: " + personagem.getNome());
				}
			}
		}

		atualizarInterfaceTotal.run();
	}

	public void salvarEstadoJogadores() {
		System.out.println("SISTEMA: Salvando estado de todos os jogadores...");
		EstadoCombate estado = estadoSupplier.get();
		if (estado == null) {
			return;
		}

		for (Personagem personagem : estado.getCombatentes()) {
			if (isPlayer.test(personagem)) {
				salvarPersonagem.accept(personagem);
			}
		}
		System.out.println("SISTEMA: Salvamento concluído.");
	}

	public Personagem recarregarPersonagem(String nomeArquivoSemExtensao) {
		System.out.println("EDITOR: Resetando alterações de " + nomeArquivoSemExtensao + "...");

		Personagem personagemRecarregado = carregarPersonagem.apply(nomeArquivoSemExtensao);
		if (personagemRecarregado == null) {
			return null;
		}

		EstadoCombate estado = estadoSupplier.get();
		if (estado != null) {
			Personagem personagemAntigo = null;
			for (Personagem personagem : estado.getCombatentes()) {
				if (personagem.getJsonFileName() != null
						&& personagem.getJsonFileName().equals(personagemRecarregado.getJsonFileName())) {
					personagemAntigo = personagem;
					break;
				}
			}

			if (personagemAntigo != null) {
				personagemRecarregado.setPosX(personagemAntigo.getPosX());
				personagemRecarregado.setPosY(personagemAntigo.getPosY());
				personagemRecarregado.setContadorTU(personagemAntigo.getContadorTU());

				estado.getCombatentes().remove(personagemAntigo);
				estado.getCombatentes().add(personagemRecarregado);
				System.out.println("EDITOR: " + nomeArquivoSemExtensao + " foi recarregado.");
			}
		}

		return personagemRecarregado;
	}
}
