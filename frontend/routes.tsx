import MainView from "Frontend/views/MainView.js";
import {createBrowserRouter, RouteObject} from "react-router-dom";
import {serverSideRoutes} from "Frontend/generated/flow/Flow";

export const routes: readonly RouteObject[] = [
  { path: "/", element: <MainView /> },
    ...serverSideRoutes
];

export const router = createBrowserRouter([...routes], {basename: new URL(document.baseURI).pathname });
