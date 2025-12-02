import { Alert, AlertColor, Snackbar } from "@mui/material";
import React, { useMemo, useState } from "react";

class ActionToastCtx {
  alertListState: [React.JSX.Element[], React.Dispatch<React.SetStateAction<React.JSX.Element[]>>];

  constructor(
    alertListState: [React.JSX.Element[], React.Dispatch<React.SetStateAction<React.JSX.Element[]>>]
  ) {
    this.alertListState = alertListState;
  }

  public raiseSuccess(message: string) {
    this.raise(message, 'success', 3000);
  }

  public raiseInfo(message: string) {
    this.raise(message, 'info', 3000);
  }

  public raiseWarning(message: string) {
    this.raise(message, 'warning', 3000);
  }

  public raiseError(message: string) {
    this.raise(message, 'error', 3000);
  }

  public raise(message: string, severity: AlertColor, autoremoveAfterMillis?: number) {
    const [alertList, setAlertList] = this.alertListState;

    const newAlert = <Alert
          onClose={()=>{}}
          severity={severity}
          variant="filled"
          sx={{ width: '100%' }}
        >
          {message}
      </Alert>;

    alertList.push(
      newAlert
    );
    setAlertList(cloneAlerts(alertList));

    setTimeout( () => this.remove(newAlert) , autoremoveAfterMillis );

  }

  remove(item: React.JSX.Element) {
    const [alertList, setAlertList] = this.alertListState;

    console.log("REMOVING", item)
    const index = alertList.indexOf(item);
    console.log(index);
    if (index > -1) {
      alertList.splice(index, 1);
      setAlertList(cloneAlerts(alertList));
    }
  }
}

function cloneAlerts(alertList: React.JSX.Element[]) : React.JSX.Element[] {
  const ret = [];
  for (const el of alertList) {
    ret.push(el);
  }
  return ret;
}

export const useActionToastCtx = () : ActionToastCtx => {
  const alertListState = useState([] as React.JSX.Element[]);

  return useMemo(() => {
    return new ActionToastCtx(alertListState);
  }, []);
};

export interface ActionToastProps {
  ctx: ActionToastCtx
}

export const ActionToast: React.FC<ActionToastProps> = ({ ctx } : ActionToastProps) => {
  const alertList = ctx.alertListState[0];
  console.log(ctx.alertListState[0]);
  return (
    <Snackbar anchorOrigin={{vertical: 'top', horizontal: 'center'}}
              onClose={()=>{}}
              open={alertList.length > 0}>
      <div>
        <div style={{'minHeight': '50px'}}></div>
        {alertList.map( alert => <>{alert}</>)}
      </div>
    </Snackbar>
  )
}
