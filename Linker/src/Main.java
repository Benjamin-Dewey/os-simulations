// Author: Benjamin Dewey
// Description: A double-pass linker.

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        linkModules(getModulesFromStandardInput());
    }

    private static Module[] getModulesFromStandardInput() {
        Scanner sc = new Scanner(System.in);

        int numOfModules = sc.nextInt();

        Module[] modules = new Module[numOfModules];

        // store the module data found in standard input
        for (int i = 0; i < numOfModules; i++) {

            // create a new module according to the corresponding module data described in standard input

            // get the new module's definitions
            int numOfDefinitions = sc.nextInt();
            SymbolAndLocationPair[] definitions = new SymbolAndLocationPair[numOfDefinitions];

            for (int j = 0; j < numOfDefinitions; j++) {
                SymbolAndLocationPair definition = new SymbolAndLocationPair(sc.next(), sc.nextInt());
                definitions[j] = definition;
            }

            // get the new module's uses
            int numOfUses = sc.nextInt();
            SymbolAndLocationPair[] uses = new SymbolAndLocationPair[numOfUses];

            for (int j = 0; j < numOfUses; j++) {
                SymbolAndLocationPair use = new SymbolAndLocationPair(sc.next(), sc.nextInt());
                uses[j] = use;
            }

            // get the new module's program text
            int numOfProgramText = sc.nextInt();
            String[] programText = new String[numOfProgramText];

            for (int j = 0; j < numOfProgramText; j++) {
                programText[j] = sc.next();
            }

            // create and store the new module
            modules[i] = new Module(definitions, uses, programText);
        }

        sc.close();

        //printModules(modules);

        return modules;
    }

    private static void linkModules(Module[] modules) {
        // prepare an array to store base addresses for each module
        int[] baseAddresses = new int[modules.length];

        // prepare an array symbol table to store absolute addresses for all symbol definitions
        int numOfSymbolDefinitions = 0;
        for (Module module : modules) {
            numOfSymbolDefinitions += module.definitions.length;
        }
        SymbolAndLocationPair[] symbolTable = new SymbolAndLocationPair[numOfSymbolDefinitions];

        // run the first pass of the linker
        linkerPassOne(modules, baseAddresses, symbolTable);

        // run the second pass of the linker
        linkerPassTwo(modules, baseAddresses, symbolTable);
    }

    private static void linkerPassOne(Module[] modules, int[] baseAddresses, SymbolAndLocationPair[] symbolTable) {
        // calculate base address and symbol table for module 0
        baseAddresses[0] = 0;
        int nextSymTabIndex = 0;
        for (int i = 0; i < modules[0].definitions.length; i++) {

            // if a definition's relative address exceeds the module size, then change the address to zero relative.
            if (modules[0].definitions[i].location > modules[0].programText.length) {
                modules[0].definitions[i].location = 0;
            }

            symbolTable[i] = modules[0].definitions[i];

            nextSymTabIndex++;
        }

        // calculate the base addresses and symbol table for all other modules
        for (int i = 1; i < modules.length; i ++) {
            baseAddresses[i] = baseAddresses[i-1] + modules[i-1].programText.length;

            for (int j = 0; j < modules[i].definitions.length; j++) {
                symbolTable[nextSymTabIndex] = modules[i].definitions[j];

                // if a definition's relative address exceeds the module size, then change the address to zero relative.
                if (symbolTable[nextSymTabIndex].location > modules[i].programText.length - 1) {
                    symbolTable[nextSymTabIndex].location = baseAddresses[i];
                    symbolTable[nextSymTabIndex].errorMessage = "Error: The definition of " + symbolTable[nextSymTabIndex].symbol + " is outside module " + i + "; zero (relative) used.";
                } else {
                    symbolTable[nextSymTabIndex].location += baseAddresses[i];
                }

                nextSymTabIndex++;
            }
        }

        printSymbolTable(symbolTable);
    }

    private static void linkerPassTwo(Module[] modules, int[] baseAddresses, SymbolAndLocationPair[] symbolTable) {
        String RELATIVE = "3";
        String IMMEDIATE = "1";
        String EXTERNAL = "4";
        int SENTINEL = 777;

        // prepare an array to store the linked program text of the modules
        int numOfProgramText = 0;
        for (Module module : modules) {
            numOfProgramText += module.programText.length;
        }
        String[] linkedProgramText = new String[numOfProgramText];

        // link the program text
        for (int i = 0; i < modules.length; i++) {
            Module module = modules[i];

            // resolve external references
            for (SymbolAndLocationPair use : module.uses) {
                // get the use's absolute address
                String absoluteAddress = "000";
                for (SymbolAndLocationPair pair : symbolTable) {
                    if (pair.symbol.equals(use.symbol)) {
                        absoluteAddress = Integer.toString(pair.location);

                        // format the absolute address to be three digits long
                        absoluteAddress = getFormattedAbsoluteAddress(absoluteAddress);

                        break;
                    }
                }

                // get the use's program text block in the module's program text
                String programTextBlock = module.programText[use.location];

                // check if the program text block has an immediate address, if so add an error message
                String immediateAddressErrorMessage = "";
                if (programTextBlock.substring(4).equals(IMMEDIATE))
                    immediateAddressErrorMessage = " Error: Immediate address on use list; treated as External.";

                // resolve the external reference of the use's program text block
                String resolvedProgramTextBlock = programTextBlock.substring(0, 1) + absoluteAddress + immediateAddressErrorMessage;

                // store the resolved program text block
                linkedProgramText[use.location + baseAddresses[i]] = resolvedProgramTextBlock;

                // check if the use is not defined and add an error message to the program text
                checkIfUseIsNotDefined(use, symbolTable, linkedProgramText, use.location + baseAddresses[i]);


                // get the location of the next program text block that references this use
                int nextLocation = Integer.parseInt(programTextBlock.substring(1, 4));

                // resolve any next locations of the external reference
                while (nextLocation != SENTINEL) {
                    programTextBlock = module.programText[nextLocation];

                    // check if the program text block has an immediate address, if so add an error message
                    immediateAddressErrorMessage = "";
                    if (programTextBlock.substring(4).equals(IMMEDIATE))
                        immediateAddressErrorMessage = " Error: Immediate address on use list; treated as External.";

                    // resolve the external reference of the use's program text block
                    resolvedProgramTextBlock = programTextBlock.substring(0, 1) + absoluteAddress + immediateAddressErrorMessage;

                    // store the resolved program text block
                    linkedProgramText[nextLocation + baseAddresses[i]] = resolvedProgramTextBlock;

                    // check if the use is not defined and add an error message to the program text
                    checkIfUseIsNotDefined(use, symbolTable, linkedProgramText,nextLocation + baseAddresses[i]);

                    nextLocation = Integer.parseInt(programTextBlock.substring(1, 4));
                }
            }

            // relocate relative addresses and format all remaining program text blocks
            for (int j = 0; j < module.programText.length; j++) {
                int index = j + baseAddresses[i];

                if (linkedProgramText[index] == null) {
                    String programTextBlock = module.programText[j];

                    if (programTextBlock.substring(4).equals(RELATIVE)) {
                        // relocate the text block
                        String relativeAddress = programTextBlock.substring(1, 4);
                        int absoluteAddressAsInt = Integer.parseInt(relativeAddress) + baseAddresses[i];

                        String absoluteAddress = Integer.toString(absoluteAddressAsInt);

                        // format the absolute address to be three digits long
                        absoluteAddress = getFormattedAbsoluteAddress(absoluteAddress);

                        programTextBlock = programTextBlock.substring(0, 1) + absoluteAddress;
                    } else if (programTextBlock.substring(4).equals(EXTERNAL)) {
                        // found an external address that was not on a use list so give an error message
                        programTextBlock = programTextBlock.substring(0, 4) + " Error: External type address not on use chain; treated as Immediate type.";
                    } else {
                        programTextBlock = programTextBlock.substring(0, 4);
                    }

                    linkedProgramText[index] = programTextBlock;
                }
            }
        }

        printLinkedProgramText(linkedProgramText);

        printWarnings(modules, symbolTable);
    }

    private static void checkIfUseIsNotDefined(SymbolAndLocationPair use, SymbolAndLocationPair[] symbolTable, String[] linkedProgramText, int index) {
        boolean useIsDefined = false;
        for (SymbolAndLocationPair pair : symbolTable) {
            if (use.symbol.equals(pair.symbol)) {
                useIsDefined = true;
                break;
            }
        }
        if (!useIsDefined) {
            linkedProgramText[index] += " Error: " + use.symbol + " is not defined; zero used." ;
        }
    }

    private static void printWarnings(Module[] modules, SymbolAndLocationPair[] symbolTable) {

        // Generate warning messages for cases in which symbol definitions are not used.
        String[] definitionNotUsedWarnings = new String[symbolTable.length];

        boolean definitionIsUsed;
        boolean didPrintWarnings = false;

        for (int i = 0; i < symbolTable.length; i++) {
            SymbolAndLocationPair pair = symbolTable[i];
            definitionIsUsed = false;

            for (Module module : modules) {
                for (SymbolAndLocationPair use : module.uses) {
                    if (pair.symbol.equals(use.symbol)) {
                        definitionIsUsed = true;
                        break;
                    }
                }
                if (definitionIsUsed) break;
            }
            if (!definitionIsUsed) {
                definitionNotUsedWarnings[i] = "Warning: Symbol " + pair.symbol + " was defined but never used.";
            }
        }

        // Print warning messages for cases in which symbol definitions are not used.
        System.out.println();
        for (String warning : definitionNotUsedWarnings) {
            if (warning != null) {
                System.out.println(warning);
                didPrintWarnings = true;
            }
        }

        if (didPrintWarnings) System.out.println();
    }

    private static void printLinkedProgramText(String[] linkedProgramText) {
        System.out.println("-Memory Map-");
        int index = 0;
        for (String programTextBlock : linkedProgramText) {
            String spacing;
            if (index > 9) spacing = " ";
            else spacing = "  ";
            System.out.println(index + ":" + spacing + programTextBlock);
            index++;
        }
    }

    private static String getFormattedAbsoluteAddress(String absoluteAddress) {
        if (absoluteAddress.length() < 3) {
            if (absoluteAddress.length() == 2) {
                absoluteAddress = "0" + absoluteAddress;
            } else if (absoluteAddress.length() == 1) {
                absoluteAddress = "00" + absoluteAddress;
            }
        }

        return absoluteAddress;
    }

    private static void printSymbolTable(SymbolAndLocationPair[] symbolTable) {

        // print the symbol table and if there are duplicate definitions print error messages

        System.out.println();

        String[] duplicateSymbols = new String[symbolTable.length];

        System.out.println("-Symbol Table-");

        for (int i = 0; i < symbolTable.length; i ++) {
            SymbolAndLocationPair pair = symbolTable[i];

            // Check if the symbol is a known duplicate to avoid printing it more than once
            boolean knownDuplicate = false;
            for (int j = 0; j < i; j++) {
                if (duplicateSymbols[j] != null) {
                    if (duplicateSymbols[j].equals(pair.symbol)) {
                        knownDuplicate = true;
                        break;
                    }
                }
            }

            if (knownDuplicate) continue; // skip the symbol

            System.out.print(pair.symbol + "=" + pair.location);

            // print an attached error message if one exists
            if (pair.errorMessage != null) System.out.print(" " + pair.errorMessage);

            // check if the symbol is an unknown duplicate
            for (SymbolAndLocationPair otherPair : symbolTable) {
                if (otherPair != pair) {
                    if (otherPair.symbol.equals(pair.symbol)) {
                        System.out.print(" Error: This variable is multiply defined; first value used.");
                        duplicateSymbols[i] = pair.symbol;
                    }
                }
            }

            System.out.println();
        }

        System.out.println();
    }
}

class Module {
    SymbolAndLocationPair[] definitions;
    SymbolAndLocationPair[] uses;
    String[] programText;

    Module(SymbolAndLocationPair[] definitions, SymbolAndLocationPair[] uses, String[] programText) {
        this.definitions = definitions;
        this.uses = uses;
        this.programText = programText;
    }
}

class SymbolAndLocationPair {
    String symbol;
    int location;
    String errorMessage;

    SymbolAndLocationPair(String symbol, int location) {
        this.symbol = symbol;
        this.location = location;
    }
}
