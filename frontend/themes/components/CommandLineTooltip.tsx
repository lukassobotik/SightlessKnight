import styles from "Frontend/themes/sightlessknight/components/commandline.module.css";
import {useEffect, useState} from "react";

export default function CommandLineTooltip({command, hideTooltip} : {command : string, hideTooltip: boolean}){
    const [commands, setCommands] = useState<string[]>([]);

    useEffect(() => {
        console.log("Command line tooltip initialized.", command);
    }, []);

    useEffect(() => {
        setCommands(predictCommand(command));
    }, [command]);

    function ignoreVariables(inputCommand: string): string {
        return inputCommand.replace(/<[^>]*>/g, "<>");
    }

    function predictCommand(inputCommand: string): string[] {
        const predictions = [];

        // Check for commands starting with "/"s
        if (inputCommand.startsWith("/")) {
            const matchedCommands = Commands.filter((cmd) => {
                let replacedVariablesWithGenericString = ignoreVariables(inputCommand);
                console.warn(cmd, replacedVariablesWithGenericString, "Matched: ", cmd.startsWith(replacedVariablesWithGenericString));
                return cmd.startsWith(replacedVariablesWithGenericString)
            }
            );
            predictions.push(...matchedCommands);
        }

        console.error(predictions);
        if (predictions.length === 0) {
            predictions.push("No commands found.");
        }

        return predictions;
    }

    return (
        hideTooltip && <div className={styles.tooltip}>
            <div className={styles.tooltip_content}>
                <p>Commands:</p>
                {commands.map((cmd, i) => <div key={i}>{cmd}</div>)}
            </div>
        </div>
    )
}

export const Commands = [
    "/undo",
    "/perft <depth>",
]