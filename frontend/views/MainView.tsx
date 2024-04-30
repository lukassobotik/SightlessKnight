import {Button} from "@hilla/react-components/Button.js";
import {Notification} from "@hilla/react-components/Notification.js";
import {TextField} from "@hilla/react-components/TextField.js";
import {PlayGameEndpoint} from "Frontend/generated/endpoints.js";
import {useEffect, useState} from "react";

export default function MainView() {
  const [name, setName] = useState("");

  useEffect(() => {
      const fetchData = async () => {
          await PlayGameEndpoint.initialize();
          await PlayGameEndpoint.printBoard();
      };
      fetchData().then(() => {
          Notification.show("Game initialized");
      });
  }, []);

  return (
    <>
      <TextField
        label="Your name"
        onValueChanged={(e) => {
          setName(e.detail.value);
        }}
      />
      <Button
        onClick={async () => {
            await PlayGameEndpoint.playMoveFromText(name);
        }}
      >
        Say hello
      </Button>
    </>
  );
}
