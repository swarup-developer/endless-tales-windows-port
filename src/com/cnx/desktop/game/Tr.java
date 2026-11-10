package com.cnx.desktop.game;

import java.util.HashMap;
import java.util.Map;

/**
 * Texts of the Windows edition's own screens (title, game menu, options, help, list words) in the
 * game's three languages. The English text is the key; a missing translation falls back to English.
 */
public final class Tr {
    private static final Map<String, String[]> T = new HashMap<>();

    private static void put(String en, String es, String pt) { T.put(en, new String[]{es, pt}); }

    static {
        // ---- title screen
        put("Continue", "Continuar", "Continuar");
        put("Choose character", "Elegir personaje", "Escolher personagem");
        put("New game", "Nueva partida", "Novo jogo");
        put("Options", "Opciones", "Opções");
        put("Quit", "Salir", "Sair");
        put("Endless Tales 2. Main menu", "Endless Tales 2. Menú principal", "Endless Tales 2. Menu principal");
        put("Windows edition  ·  game data v2.0.0.3  ·  F1: keyboard help",
                "Edición para Windows  ·  datos v2.0.0.3  ·  F1: ayuda de teclado",
                "Edição para Windows  ·  dados v2.0.0.3  ·  F1: ajuda do teclado");
        put("Characters", "Personajes", "Personagens");
        put("Play", "Jugar", "Jogar");
        put("Play %s, %s, level %d", "Jugar con %s, %s, nivel %d", "Jogar com %s, %s, nível %d");
        put("Delete", "Borrar", "Apagar");
        put("Delete %s", "Borrar a %s", "Apagar %s");
        put("Delete character", "Borrar personaje", "Apagar personagem");
        put("Delete %s? This cannot be undone.", "¿Borrar a %s? No se puede deshacer.", "Apagar %s? Isso não pode ser desfeito.");
        put("Back", "Volver", "Voltar");

        // ---- game menu
        put("Menu", "Menú", "Menu");
        put("Resume", "Seguir jugando", "Continuar jogando");
        put("Save game", "Guardar partida", "Salvar jogo");
        put("Game saved", "Partida guardada", "Jogo salvo");
        put("Keyboard help", "Ayuda de teclado", "Ajuda do teclado");
        put("Save and return to title", "Guardar y volver al inicio", "Salvar e voltar ao início");
        put("Save and quit", "Guardar y salir", "Salvar e sair");

        // ---- options
        put("Speech", "Voz", "Voz");
        put("Speech output", "Salida de voz", "Saída de voz");
        put("Automatic: NVDA, otherwise the Windows voice", "Automática: NVDA, si no, la voz de Windows", "Automática: NVDA, senão a voz do Windows");
        put("Screen reader only", "Solo lector de pantalla", "Só leitor de tela");
        put("Windows voice", "Voz de Windows", "Voz do Windows");
        put("Off", "Desactivada", "Desligada");
        put("Speech output, Off. Press Left arrow to turn speech back on.",
                "Salida de voz, desactivada. Pulsa flecha izquierda para volver a activarla.",
                "Saída de voz, desligada. Pressione seta para a esquerda para ligá-la de novo.");
        put("Windows voice speed", "Velocidad de la voz de Windows", "Velocidade da voz do Windows");
        put("normal", "normal", "normal");
        put("faster, plus %d", "más rápida, más %d", "mais rápida, mais %d");
        put("slower, minus %d", "más lenta, menos %d", "mais lenta, menos %d");
        put("Say position in lists, like 2 of 5", "Decir la posición en las listas, como 2 de 5", "Dizer a posição nas listas, como 2 de 5");
        put("Sound when moving through lists", "Sonido al moverse por las listas", "Som ao mover pelas listas");
        put("Battle", "Combate", "Combate");
        put("Critical hits in battle: press Space quickly (off: automatic)",
                "Golpes críticos en combate: pulsa Espacio rápido (desactivado: automático)",
                "Golpes críticos em combate: pressione Espaço rápido (desligado: automático)");
        put("Sound", "Sonido", "Som");
        put("Music volume", "Volumen de la música", "Volume da música");
        put("Sound effects volume", "Volumen de los efectos", "Volume dos efeitos");
        put("%d percent", "%d por ciento", "%d por cento");
        put("Display and language", "Pantalla e idioma", "Tela e idioma");
        put("Text speed on screen", "Velocidad del texto en pantalla", "Velocidade do texto na tela");
        put("instant", "instantáneo", "instantâneo");
        put("Full screen", "Pantalla completa", "Tela cheia");
        put("Window", "Ventana", "Janela");
        put("Language", "Idioma", "Idioma");
        put("Done", "Listo", "Pronto");

        put("Event", "Evento", "Evento");

        // ---- new game
        put("Character name", "Nombre del personaje", "Nome do personagem");
        put("Press Enter to type", "Pulsa Intro para escribir", "Pressione Enter para digitar");
        put("Class", "Clase", "Classe");
        put("Gender", "Género", "Gênero");
        put("Portrait", "Retrato", "Retrato");
        put("Left and Right to change.", "Izquierda y derecha para cambiar.", "Esquerda e direita para mudar.");
        put("Begin adventure", "Comenzar la aventura", "Começar a aventura");
        put("Starting stats: health %d, mana %d", "Atributos iniciales: salud %d, maná %d", "Atributos iniciais: vida %d, mana %d");

        // ---- game screens (battle, shop, inventory, crafting, house, map, skills, pop-ups)
        put("Nothing to repeat", "Nada que repetir", "Nada para repetir");
        put("Error: ", "Error: ", "Erro: ");
        put("Yes", "Sí", "Sim");
        put("No", "No", "Não");
        put(": this screen is not in the Windows version yet.", ": esta pantalla aún no está en la versión de Windows.", ": esta tela ainda não está na versão para Windows.");
        put("no screen reader running", "ningún lector de pantalla activo", "nenhum leitor de tela ativo");
        put("Battle! ", "¡Combate! ", "Combate! ");
        put(", health ", ", salud ", ", vida ");
        put(" of ", " de ", " de ");
        put("Attack", "Atacar", "Atacar");
        put("Skills", "Habilidades", "Habilidades");
        put("Items", "Objetos", "Itens");
        put("Escape", "Huir", "Fugir");
        put("Battle status", "Estado del combate", "Estado do combate");
        put("Your turn", "Tu turno", "Sua vez");
        put("health ", "salud ", "vida ");
        put("Choose a target", "Elige un objetivo", "Escolha um alvo");
        put("Critical hit!", "¡Golpe crítico!", "Golpe crítico!");
        put("Critical chance! Press Space as fast as you can!", "¡Oportunidad de crítico! ¡Pulsa Espacio lo más rápido que puedas!", "Chance de crítico! Pressione Espaço o mais rápido que puder!");
        put("Critical! Press Space!", "¡Crítico! ¡Pulsa Espacio!", "Crítico! Pressione Espaço!");
        put("Strike!", "¡Golpear!", "Golpear!");
        put(" MP", " PM", " PM");
        put(" HP", " PV", " PV");
        put(": health ", ": salud ", ": vida ");
        put(", mana ", ", maná ", ", mana ");
        put(": defeated. ", ": derrotado. ", ": derrotado. ");
        put("Turn ", "Turno ", "Turno ");
        put("   HP ", "   PV ", "   PV ");
        put("   MP ", "   PM ", "   PM ");
        put("Empty.", "Vacío.", "Vazio.");
        put("Stats", "Atributos", "Atributos");
        put("Quests: ", "Misiones: ", "Missões: ");
        put(" active, ", " activas, ", " ativas, ");
        put(" completed", " completadas", " concluídas");
        put("Inventory: ", "Inventario: ", "Inventário: ");
        put(" kinds of items", " tipos de objetos", " tipos de itens");
        put("Gold", "Oro", "Ouro");
        put("Skill points", "Puntos de habilidad", "Pontos de habilidade");
        put("No quests yet.", "Aún no hay misiones.", "Ainda não há missões.");
        put("Goodbye", "Adiós", "Tchau");
        put("Your reply", "Tu respuesta", "Sua resposta");
        put("You cannot leave this conversation yet", "Aún no puedes dejar esta conversación", "Você ainda não pode sair desta conversa");
        put("Ingredient removed", "Ingrediente quitado", "Ingrediente retirado");
        put("You have no ingredients", "No tienes ingredientes", "Você não tem ingredientes");
        put("Choose an ingredient for slot ", "Elige un ingrediente para el espacio ", "Escolha um ingrediente para o espaço ");
        put("Success! You made ", "¡Éxito! Has hecho ", "Sucesso! Você fez ");
        put("It failed. ", "Ha fallado. ", "Falhou. ");
        put(" was not made, and the ingredients were used.", " no se hizo y los ingredientes se gastaron.", " não foi feito e os ingredientes foram gastos.");
        put("Alchemy", "Alquimia", "Alquimia");
        put("Cooking", "Cocina", "Culinária");
        put("Crafting", "Fabricación", "Fabricação");
        put("Choose up to three ingredients. When they make something, its name and chance of success are read. ", "Elige hasta tres ingredientes. Cuando formen algo, se leerá su nombre y la probabilidad de éxito. ", "Escolha até três ingredientes. Quando formarem algo, o nome e a chance de sucesso serão lidos. ");
        put("Your skill adds ", "Tu habilidad suma ", "Sua habilidade soma ");
        put(" percent.", " por ciento.", " por cento.");
        put("Slot ", "Espacio ", "Espaço ");
        put("Enter to choose an ingredient", "Intro para elegir un ingrediente", "Enter para escolher um ingrediente");
        put("Enter to take it out", "Intro para quitarlo", "Enter para retirá-lo");
        put("Makes ", "Hace ", "Faz ");
        put(" percent chance plus ", " por ciento de probabilidad más ", " por cento de chance mais ");
        put(" from skill, takes ", " por habilidad, tarda ", " pela habilidade, leva ");
        put(" minutes", " minutos", " minutos");
        put("Cook", "Cocinar", "Cozinhar");
        put("Mix", "Mezclar", "Misturar");
        put("Craft", "Fabricar", "Fabricar");
        put("No ingredients yet", "Aún no hay ingredientes", "Ainda não há ingredientes");
        put("These ingredients make nothing", "Estos ingredientes no forman nada", "Esses ingredientes não formam nada");
        put("Clear all slots", "Vaciar todos los espacios", "Esvaziar todos os espaços");
        put(" It is night.", " Es de noche.", " É noite.");
        put("Map", "Mapa", "Mapa");
        put("character, inventory, skills", "personaje, inventario, habilidades", "personagem, inventário, habilidades");
        put("Character sheet", "Ficha del personaje", "Ficha do personagem");
        put("stats and quests", "atributos y misiones", "atributos e missões");
        put("Inventory", "Inventario", "Inventário");
        put("Equipment", "Equipo", "Equipamento");
        put(" skill points to spend", " puntos de habilidad para gastar", " pontos de habilidade para gastar");
        put("Mounts", "Monturas", "Montarias");
        put(" min", " min", " min");
        put("Error", "Error", "Erro");
        put("The game data reported an error and the game must return to the title screen.\n", "Los datos del juego informaron un error y el juego debe volver a la pantalla de inicio.\n", "Os dados do jogo relataram um erro e o jogo precisa voltar à tela inicial.\n");
        put("Your last save is kept.", "Tu última partida guardada se conserva.", "Seu último jogo salvo foi mantido.");
        put("All ", "Todo ", "Tudo ");
        put("Took ", "Sacaste ", "Pegou ");
        put("Stored ", "Guardaste ", "Guardou ");
        put("How many ", "¿Cuántos ", "Quantos ");
        put(" is empty", " está vacío", " está vazio");
        put("Your bag is empty", "Tu bolsa está vacía", "Sua bolsa está vazia");
        put("Take from ", "Sacar de ", "Pegar de ");
        put("Put in ", "Guardar en ", "Guardar em ");
        put("You are not riding a mount.", "No vas montado.", "Você não está montado.");
        put("Riding ", "Montando ", "Montando ");
        put(". Speed ", ". Velocidad ", ". Velocidade ");
        put(". Saddlebag carries ", ". La alforja lleva ", ". O alforje carrega ");
        put(", too heavy", ", demasiado peso", ", peso demais");
        put("You have no mounts. Stables sell them.", "No tienes monturas. Los establos las venden.", "Você não tem montarias. Os estábulos as vendem.");
        put("Riding: ", "Montando: ", "Montando: ");
        put("Ride ", "Montar ", "Montar ");
        put("speed ", "velocidad ", "velocidade ");
        put("Now riding ", "Ahora montas ", "Agora montando ");
        put("Take from saddlebag", "Sacar de la alforja", "Pegar do alforje");
        put("Saddlebag", "Alforja", "Alforje");
        put("Put in saddlebag", "Guardar en la alforja", "Guardar no alforje");
        put("Take from chest", "Sacar del cofre", "Pegar do baú");
        put("Chest", "Cofre", "Baú");
        put("Put in chest", "Guardar en el cofre", "Guardar no baú");
        put("Leave the house", "Salir de la casa", "Sair da casa");
        put("You have ", "Tienes ", "Você tem ");
        put("Worn on: ", "Se lleva en: ", "Usado em: ");
        put("Damage ", "Daño ", "Dano ");
        put("Defense ", "Defensa ", "Defesa ");
        put("Critical chance ", "Probabilidad de crítico ", "Chance de crítico ");
        put(" percent. ", " por ciento. ", " por cento. ");
        put("Satisfies hunger. ", "Quita el hambre. ", "Mata a fome. ");
        put("Only in battle. ", "Solo en combate. ", "Só em combate. ");
        put("Weight ", "Peso ", "Peso ");
        put("Read", "Leer", "Ler");
        put("Read (you need to know how to read)", "Leer (necesitas saber leer)", "Ler (você precisa saber ler)");
        put("Use", "Usar", "Usar");
        put("Equip", "Equipar", "Equipar");
        put("Discard", "Tirar", "Descartar");
        put(". Health ", ". Salud ", ". Vida ");
        put("Cannot equip: ", "No se puede equipar: ", "Não é possível equipar: ");
        put("Equipped ", "Equipado: ", "Equipado: ");
        put("Discard 1", "Tirar 1", "Descartar 1");
        put("Discard ", "Tirar ", "Descartar ");
        put("Discard all ", "Tirar todos: ", "Descartar todos: ");
        put("Cancel", "Cancelar", "Cancelar");
        put("Enter to take off", "Intro para quitártelo", "Enter para tirar");
        put("Took off ", "Te quitaste ", "Tirou ");
        put("Nothing equipped. ", "Nada equipado. ", "Nada equipado. ");
        put("Attack ", "Ataque ", "Ataque ");
        put(" to ", " a ", " a ");
        put("All items", "Todos los objetos", "Todos os itens");
        put("Consumables", "Consumibles", "Consumíveis");
        put("Other items", "Otros objetos", "Outros itens");
        put(" kilograms", " kilos", " quilos");
        put(", too heavy, you move slowly", ", demasiado peso, te mueves despacio", ", peso demais, você anda devagar");
        put("Carrying ", "Llevas ", "Carregando ");
        put(". Food ", ". Comida ", ". Comida ");
        put(" percent", " por ciento", " por cento");
        put(", hungry", ", con hambre", ", com fome");
        put("Show: ", "Mostrar: ", "Mostrar: ");
        put("Enter to change", "Intro para cambiar", "Enter para mudar");
        put("No items", "No hay objetos", "Nenhum item");
        put("Here: ", "Aquí: ", "Aqui: ");
        put("Elsewhere in ", "En otras partes de ", "Em outras partes de ");
        put("No known creatures.", "No se conocen criaturas.", "Nenhuma criatura conhecida.");
        put("Unknown region", "Región desconocida", "Região desconhecida");
        put("You do not have the map of this region.", "No tienes el mapa de esta región.", "Você não tem o mapa desta região.");
        put("You are at ", "Estás en ", "Você está em ");
        put(" places: ", " lugares: ", " lugares: ");
        put("Region ", "Región ", "Região ");
        put("Region: ", "Región: ", "Região: ");
        put("Left and Right to change region", "Izquierda y derecha para cambiar de región", "Esquerda e direita para mudar de região");
        put("Road to ", "Camino a ", "Caminho para ");
        put("To travel, use Destinations on the main screen.", "Para viajar, usa Destinos en la pantalla principal.", "Para viajar, use Destinos na tela principal.");
        put("Bestiary", "Bestiario", "Bestiário");
        put("creatures of this region", "criaturas de esta región", "criaturas desta região");
        put("You learned new skills!", "¡Aprendiste nuevas habilidades!", "Você aprendeu novas habilidades!");
        put("Level up!", "¡Subiste de nivel!", "Subiu de nível!");
        put("You have been defeated. You wake up at your residence.", "Has sido derrotado. Despiertas en tu residencia.", "Você foi derrotado. Você acorda na sua residência.");
        put("Defeated", "Derrotado", "Derrotado");
        put("Experience", "Experiencia", "Experiência");
        put("Gems", "Gemas", "Gemas");
        put("Quest completed", "Misión completada", "Missão concluída");
        put("New quest", "Nueva misión", "Nova missão");
        put("Nothing for you here", "Aquí no hay nada para ti", "Não há nada para você aqui");
        put(", you have ", ", tienes ", ", você tem ");
        put(" gold each", " de oro cada uno", " de ouro cada");
        put("This shop buys nothing you carry", "Esta tienda no compra nada de lo que llevas", "Esta loja não compra nada do que você carrega");
        put("Sell, you have ", "Vender, tienes ", "Vender, você tem ");
        put("Price ", "Precio ", "Preço ");
        put("Sells for ", "Se vende por ", "Vende por ");
        put(" gold. You have ", " de oro. Tienes ", " de ouro. Você tem ");
        put("Buy", "Comprar", "Comprar");
        put(" for ", " por ", " por ");
        put(". You have ", ". Tienes ", ". Você tem ");
        put("Trade not possible", "No se puede comerciar", "Troca não é possível");
        put("you own this mount", "ya tienes esta montura", "você já tem esta montaria");
        put(" and ", " y ", " e ");
        put(" gems", " gemas", " gemas");
        put(", speed ", ", velocidad ", ", velocidade ");
        put(", carries ", ", lleva ", ", carrega ");
        put("Not possible", "No es posible", "Não é possível");
        put("Keeper: ", "Encargado: ", "Responsável: ");
        put(" gold.", " de oro.", " de ouro.");
        put("Buy ", "Comprar ", "Comprar ");
        put("Sell", "Vender", "Vender");
        put("your items", "tus objetos", "seus itens");
        put("Leave", "Salir", "Sair");
        put(" gold", " de oro", " de ouro");
        put(", too expensive", ", demasiado caro", ", caro demais");
        put("Health ", "Salud ", "Vida ");
        put(" skill points. Use Left and Right on an attribute to raise or lower it, then choose Confirm.", " puntos de habilidad. Usa izquierda y derecha en un atributo para subirlo o bajarlo, y luego elige Confirmar.", " pontos de habilidade. Use esquerda e direita num atributo para aumentar ou diminuir, depois escolha Confirmar.");
        put(" skill points left.", " puntos de habilidad restantes.", " pontos de habilidade restantes.");
        put("next costs ", "el siguiente cuesta ", "o próximo custa ");
        put("Confirm", "Confirmar", "Confirmar");
        put(" points left", " puntos restantes", " pontos restantes");
        put("Improve attributes, ", "Mejorar atributos, ", "Melhorar atributos, ");
        put(" points", " puntos", " pontos");
        put("Cannot go below ", "No puede bajar de ", "Não pode ficar abaixo de ");
        put("Attributes saved.", "Atributos guardados.", "Atributos salvos.");
        put("Maximum health", "Salud máxima", "Vida máxima");
        put("Maximum mana", "Maná máximo", "Mana máxima");
        put(" skills. ", " habilidades. ", " habilidades. ");
        put(" skill points to spend.", " puntos de habilidad para gastar.", " pontos de habilidade para gastar.");
        put("Improve attributes", "Mejorar atributos", "Melhorar atributos");
        put(" skill points", " puntos de habilidad", " pontos de habilidade");
        put(", usable now", ", se puede usar ahora", ", pode usar agora");
        put(" mana", " de maná", " de mana");
        put(" health", " de salud", " de vida");
        put(" skill", " habilidad", " habilidade");
        put(". Costs ", ". Cuesta ", ". Custa ");
        put("Target: ", "Objetivo: ", "Alvo: ");
        put("Power ", "Poder ", "Poder ");
        put("Use now", "Usar ahora", "Usar agora");

        put("Age", "Edad", "Idade");
        put("empty", "vacío", "vazio");
        put("point", "punto", "ponto");
        put("points", "puntos", "pontos");

        // ---- list words spoken everywhere
        put("on", "activado", "ligado");
        put("off", "desactivado", "desligado");
        put("%d of %d", "%d de %d", "%d de %d");
        put("unavailable", "no disponible", "indisponível");
        put("Nothing here", "No hay nada aquí", "Não há nada aqui");
        put("No more sections", "No hay más secciones", "Não há mais seções");
        put("Please wait", "Espera, por favor", "Aguarde, por favor");
        put("No game in progress", "No hay ninguna partida en curso", "Nenhum jogo em andamento");
        put("Close", "Cerrar", "Fechar");
        put("Editing %s.", "Editando %s.", "Editando %s.");
        put("Empty", "Vacío", "Vazio");
        put("Type, then press Enter.", "Escribe y pulsa Intro.", "Digite e pressione Enter.");
        put("space", "espacio", "espaço");
        put("Maximum 20 letters", "Máximo 20 letras", "Máximo de 20 letras");

        // ---- keyboard help
        put("Up and Down arrows, or Tab: move through the choices.",
                "Flechas arriba y abajo, o Tab: moverse por las opciones.",
                "Setas para cima e para baixo, ou Tab: mover pelas opções.");
        put("Enter or Space: choose.", "Intro o Espacio: elegir.", "Enter ou Espaço: escolher.");
        put("Escape or Backspace: go back, or open the game menu.",
                "Escape o Retroceso: volver, o abrir el menú del juego.",
                "Escape ou Backspace: voltar, ou abrir o menu do jogo.");
        put("Left and Right arrows: change a setting.", "Flechas izquierda y derecha: cambiar un ajuste.", "Setas para a esquerda e para a direita: mudar uma configuração.");
        put("Home and End: first and last choice. Page Up and Page Down: jump between sections.",
                "Inicio y Fin: primera y última opción. Re Pág y Av Pág: saltar entre secciones.",
                "Home e End: primeira e última opção. Page Up e Page Down: pular entre seções.");
        put("Shift plus a letter: jump to the next choice starting with that letter.",
                "Mayúsculas más una letra: ir a la siguiente opción que empieza con esa letra.",
                "Shift mais uma letra: ir para a próxima opção que começa com essa letra.");
        put("R: repeat the current text.", "R: repetir el texto actual.", "R: repetir o texto atual.");
        put("S: your status: health, mana, gold, level, day, time and place.",
                "S: tu estado: salud, maná, oro, nivel, día, hora y lugar.",
                "S: seu estado: vida, mana, ouro, nível, dia, hora e lugar.");
        put("C: character sheet with stats, quests and inventory.",
                "C: ficha del personaje con atributos, misiones e inventario.",
                "C: ficha do personagem com atributos, missões e inventário.");
        put("I: inventory, to use, equip or discard items.",
                "I: inventario, para usar, equipar o tirar objetos.",
                "I: inventário, para usar, equipar ou descartar itens.");
        put("Control: stop speech.", "Control: callar la voz.", "Control: parar a voz.");
        put("F1: this help.", "F1: esta ayuda.", "F1: esta ajuda.");
        put("F11: full screen.", "F11: pantalla completa.", "F11: tela cheia.");

        // ---- S key status
        put("level", "nivel", "nível");
        put("Health %d of %d.", "Salud %d de %d.", "Vida %d de %d.");
        put("Mana %d of %d.", "Maná %d de %d.", "Mana %d de %d.");
        put("Experience %d", "Experiencia %d", "Experiência %d");
        put(" of %d for the next level", " de %d para el siguiente nivel", " de %d para o próximo nível");
        put("Gold %d.", "Oro %d.", "Ouro %d.");
        put("Day %d, %s", "Día %d, %s", "Dia %d, %s");
        put(", daytime.", ", de día.", ", de dia.");
        put(", night.", ", de noche.", ", de noite.");
        put("At %s.", "En %s.", "Em %s.");
        put("You are hungry.", "Tienes hambre.", "Você está com fome.");
        put("Poisoned.", "Envenenado.", "Envenenado.");
        put("Bleeding.", "Sangrando.", "Sangrando.");
        put("Carrying too much.", "Llevas demasiado peso.", "Carregando peso demais.");
    }

    private Tr() {}

    /** The text in the current language. */
    public static String t(String en) {
        String[] v = T.get(en);
        if (v == null) return en;
        return switch (Settings.language) { case "es" -> v[0]; case "pt" -> v[1]; default -> en; };
    }

    /** The text in the current language, with %s / %d filled in. */
    public static String f(String en, Object... args) { return String.format(t(en), args); }

    /** True when every English key has both translations (used by the self-test). */
    static int size() { return T.size(); }
}
