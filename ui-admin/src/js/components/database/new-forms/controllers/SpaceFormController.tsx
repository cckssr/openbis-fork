import React, { useEffect, useState } from 'react';
import openbis from '@srcV3/openbis.esm';

export function SpaceFormController({ openbisFacade }: { openbisFacade: openbis.openbis }) {
  const [json, setJson] = useState<any>(null);

  useEffect(() => {
    async function fetchSpaces() {
      const spaces = await openbisFacade.getSpaces(
        [new openbisFacade.SpacePermId('DEFAULT')],
        new openbisFacade.SpaceFetchOptions()
      );
      console.log('spaces: ', spaces);
      setJson(spaces['DEFAULT']);
    }
    fetchSpaces();
    console.log(new openbisFacade.SpacePermId('DEFAULT'))
  }, [openbisFacade]);

  return (
    <div>{JSON.stringify(json || {}, null, 2)}</div>
  );
}