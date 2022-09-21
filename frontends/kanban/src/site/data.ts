import { random } from 'lodash'

const juxterData = [
  {
    id: 'https://home.juxt.site/home/people/6036b355-566d-4e24-b5e4-a1c11d8e3a6b',
    name: 'Kathryn McAllister',
    staffRecord: {
      juxtcode: 'kat'
    }
  },
  {
    id: 'https://home.juxt.site/home/people/613744a5-edd5-436d-8cc6-d778205ef600',
    name: 'Eirini Chatzidaki',
    github: 'Nionions',
    staffRecord: {
      juxtcode: 'eix'
    }
  },
  {
    id: 'https://home.juxt.site/home/people/6137jj5-edd5-436d-8cc6-d778205ef600',
    name: 'Petria Gatziou',
    github: 'petriapuipui',
    staffRecord: {
      juxtcode: 'pui'
    }
  },
  {
    id: 'https://home.juxt.site/home/people/608ab01e-8183-418b-8d19-9bf85f6d29d1',
    name: 'Joe Littlejohn',
    github: 'joelittlejohn',
    staffRecord: {
      juxtcode: 'joe'
    }
  },
  {
    id: 'https://home.juxt.site/home/people/2d2fe36d-064c-4a39-94ed-73dba30e8dd4',
    name: 'Tom Dalziel',
    github: 'tomdl89',
    staffRecord: {
      juxtcode: 'tom'
    }
  },
  {
    id: 'https://home.juxt.site/home/people/b5ff5af7-1cb0-4d4e-8435-dbf5517085d0',
    name: 'Malcolm Sparks',
    github: 'malcolmsparks',
    staffRecord: {
      juxtcode: 'mal'
    }
  },
  {
    id: 'https://home.juxt.site/home/people/2d8ced05-12c7-4603-b263-b80f2b7bd958',
    name: 'Jon Pither',
    github: 'jonpither',
    staffRecord: {
      juxtcode: 'jon'
    }
  },
  {
    id: `random${random(1, 10000)}`,
    github: 'asljuxt',
    name: 'Asel Kitulagoda',
    staffRecord: {
      juxtcode: 'asl'
    }
  },
  {
    id: `random${random(1, 10000)}`,
    github: 'chopptimus',
    name: 'Hugo Young',
    staffRecord: {
      juxtcode: 'hjy'
    }
  },
  {
    id: `random${random(1, 10000)}`,
    github: 'MaxGW',
    name: 'Max Grant-Walker',
    staffRecord: {
      juxtcode: 'max'
    }
  },
  {
    id: 'https://home.juxt.site/home/people/7f8503eb-ba07-4939-8e39-8b53a581d845',
    github: 'TheKotlinDev',
    name: 'John Mone',
    staffRecord: {
      juxtcode: 'jmo'
    }
  },

  {
    id: 'https://home.juxt.site/home/people/5b310cf8-d56b-42bc-af89-d0f08f40001b',
    name: 'Johanna Antonelli',
    github: 'johantonelli',
    staffRecord: {
      juxtcode: 'joa'
    }
  },
  {
    id: 'https://home.juxt.site/home/people/60014770-b714-4b5c-bc01-d727d36b6d9f',
    name: 'Tim Greene',
    github: 'tggreene',
    staffRecord: {
      juxtcode: 'tim'
    }
  },

  {
    id: 'https://home.juxt.site/home/people/5f3bc8b4-6c2f-46b3-8dfd-6efa88e71fc9',
    name: 'Peter Baker',
    staffRecord: {
      juxtcode: 'pbk'
    }
  },
  {
    id: 'https://home.juxt.site/home/people/5c52d95a-136b-4e2c-8224-72c369cf7a15',
    name: 'Jeremy Taylor',
    staffRecord: {
      juxtcode: 'jdt'
    }
  },

  {
    id: 'https://home.juxt.site/home/people/60f5f6c6-7990-4743-9786-9b2e786876e4',
    name: 'Renzo Borgatti',
    github: 'reborg',
    staffRecord: {
      juxtcode: 'ren'
    }
  },

  {
    id: 'https://home.juxt.site/home/people/59e47083-c60e-4cc4-bd82-9733d065bb20',
    name: 'Lucio DAlessandro',
    github: 'luciodale',
    staffRecord: {
      juxtcode: 'lda'
    }
  },
  {
    id: 'https://home.juxt.site/home/people/59e47083-c60e-4cc4-bd82-9733d065bb20',
    name: 'Alex Davis',
    github: 'armincerf',
    staffRecord: {
      juxtcode: 'alx'
    }
  },
  {
    id: 'https://home.juxt.site/home/people/5f4f73bf-1330-4642-989a-107d2037ec3b',
    name: 'Andrea Crotti',
    github: 'AndreaCrotti',
    staffRecord: {
      juxtcode: 'anc'
    }
  },
  {
    id: 'https://home.juxt.site/home/people/7f8503eb-ba07-4939-8e39-8b53a581d845',
    github: 'mbutlerw',
    name: 'Matthew Butler-Williams',
    staffRecord: {
      juxtcode: 'mat'
    }
  }
]

const assocAvatar = (mention: typeof juxterData[0]) => {
  const { github, name } = mention
  const avatar = github
    ? `https://github.com/${github}.png?size=200`
    : `https://eu.ui-avatars.com/api/?name=${name}&size=200`
  return { ...mention, avatar }
}

export const juxters = juxterData.map(assocAvatar)

const basicRoles: Record<string, string[]> = {
  HiringAdmins: ['admin', 'eix', 'tek', 'kat', 'hbr'],
  DecisionMakers: ['admin', 'jon', 'mal', 'joe'],
  Founders: ['admin', 'jon', 'mal']
}

export const roles: Record<string, string[]> = {
  ...basicRoles,
  Interviewers: juxters
    .map((j) => j.staffRecord.juxtcode)
    .filter((s) => !basicRoles['HiringAdmins'].includes(s))
    .concat(['admin'])
}

export function userAvatar(username?: string | null): string {
  return (
    juxters.find((juxter) => juxter.staffRecord.juxtcode === username)
      ?.avatar ?? ''
  )
}

export const hiringWorkflowId = 'WorkflowHiring'
